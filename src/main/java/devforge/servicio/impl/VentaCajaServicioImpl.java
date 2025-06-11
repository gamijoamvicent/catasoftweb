package devforge.servicio.impl;

import devforge.model.Caja;
import devforge.model.Cliente;
import devforge.model.Credito;
import devforge.model.Venta;
import devforge.model.VentaCaja;
import devforge.model.Producto;
import devforge.model.enums.MetodoPago;
import devforge.model.enums.TipoVenta;
import devforge.model.PrecioDolar;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import devforge.repository.CajaRepository;
import devforge.repository.VentaCajaRepository;
import devforge.repository.VentaRepository;
import devforge.repository.ProductoRepository;
import devforge.servicio.VentaCajaServicio;
import devforge.config.LicoreriaContext;
import devforge.servicio.PrecioDolarServicio;
import devforge.web.dto.VentaCajaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VentaCajaServicioImpl implements VentaCajaServicio {

    private final VentaCajaRepository ventaCajaRepository;
    private final VentaRepository ventaRepository;
    private final CajaRepository cajaRepository;
    private final ProductoRepository productoRepository;
    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public VentaCajaServicioImpl(
            VentaCajaRepository ventaCajaRepository,
            VentaRepository ventaRepository,
            CajaRepository cajaRepository,
            ProductoRepository productoRepository,
            PrecioDolarServicio precioDolarServicio,
            LicoreriaContext licoreriaContext) {
        this.ventaCajaRepository = ventaCajaRepository;
        this.ventaRepository = ventaRepository;
        this.cajaRepository = cajaRepository;
        this.productoRepository = productoRepository;
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registrarVenta(List<Map<String, Object>> items) {
        registrarVenta(items, "CONTADO", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registrarVenta(List<Map<String, Object>> items, String tipoVentaStr, Long clienteId) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("No hay items para procesar");
        }

        // Validar el tipo de venta
        if (tipoVentaStr == null) {
            tipoVentaStr = "CONTADO";
        }

        // Crear la venta
        Venta venta = new Venta();
        venta.setLicoreria(licoreriaContext.getLicoreriaActual());
        venta.setFechaVenta(LocalDateTime.now());
        venta.setTipoVenta(TipoVenta.valueOf(tipoVentaStr));
        venta.setMetodoPago(MetodoPago.EFECTIVO);
        venta.setAnulada(false);
        venta.setTotalVenta(BigDecimal.ZERO);
        venta.setTotalVentaBs(BigDecimal.ZERO);

        // Si es venta a crédito, asociar cliente
        if (venta.getTipoVenta() == TipoVenta.CREDITO) {
            if (clienteId == null) {
                throw new RuntimeException("Para ventas a crédito se requiere seleccionar un cliente");
            }
            try {
                Cliente cliente = entityManager.find(Cliente.class, clienteId);
                if (cliente == null) {
                    throw new RuntimeException("El cliente seleccionado no existe");
                }
                if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
                    throw new RuntimeException("El cliente seleccionado no tiene nombre definido");
                }
                venta.setCliente(cliente);
            } catch (Exception e) {
                throw new RuntimeException("Error al obtener el cliente: " + e.getMessage());
            }
        }

        // Guardar la venta
        venta = ventaRepository.save(venta);

        BigDecimal totalVenta = BigDecimal.ZERO;
        BigDecimal totalVentaBs = BigDecimal.ZERO;

        try {
            // Procesar cada caja
            for (Map<String, Object> item : items) {
                Long cajaId = Long.valueOf(item.get("cajaId").toString());
                int cantidad = Integer.valueOf(item.get("cantidad").toString());
                double precio = Double.valueOf(item.get("precio").toString());
                String tipoTasa = (String) item.get("tipoTasa");

                // Obtener la caja
                Caja caja = cajaRepository.findById(cajaId)
                    .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

                // Verificar que el producto asociado esté activo
                if (caja.getProducto() != null && !caja.getProducto().isActivo()) {
                    throw new RuntimeException("Stock insuficiente: El producto '" + caja.getProducto().getNombre() + 
                        "' asociado a la caja '" + caja.getNombre() + "' está inactivo.");
                }

                // Crear el detalle de venta de caja
                VentaCaja ventaCaja = new VentaCaja();
                ventaCaja.setVenta(venta);
                ventaCaja.setCaja(caja);
                ventaCaja.setCantidad(cantidad);
                ventaCaja.setPrecioUnitario(BigDecimal.valueOf(precio));
                ventaCaja.setSubtotal(BigDecimal.valueOf(precio * cantidad));
                ventaCaja.setTipoTasa(PrecioDolar.TipoTasa.valueOf(tipoTasa));
                ventaCaja.setFechaCreacion(LocalDateTime.now());

                // Calcular subtotal en bolívares
                double tasaCambio = obtenerTasaCambio(tipoTasa);
                ventaCaja.setTasaCambioUsado(BigDecimal.valueOf(tasaCambio));
                ventaCaja.setSubtotalBolivares(
                    ventaCaja.getSubtotal().multiply(BigDecimal.valueOf(tasaCambio))
                );

                // Guardar el detalle
                ventaCajaRepository.save(ventaCaja);

                // Actualizar totales de la venta
                totalVenta = totalVenta.add(ventaCaja.getSubtotal());
                totalVentaBs = totalVentaBs.add(ventaCaja.getSubtotalBolivares());

                // Descontar stock
                descontarStockCaja(cajaId, cantidad);
            }

            // Actualizar venta con totales finales
            venta.setTotalVenta(totalVenta);
            venta.setTotalVentaBs(totalVentaBs);
            ventaRepository.save(venta);

            // Si es venta a crédito, crear registro de crédito
            if (venta.getTipoVenta() == TipoVenta.CREDITO) {
                if (venta.getCliente() == null) {
                    throw new RuntimeException("No se puede crear un crédito sin cliente");
                }
                if (venta.getCliente().getNombre() == null) {
                    throw new RuntimeException("El cliente no tiene nombre definido");
                }

                Credito credito = new Credito();
                credito.setVenta(venta);
                credito.setCliente(venta.getCliente());
                credito.setLicoreria(venta.getLicoreria());
                credito.setMontoTotal(venta.getTotalVenta());
                credito.setSaldoPendiente(venta.getTotalVenta());
                credito.setFechaLimitePago(LocalDateTime.now().plusDays(30)); // 30 días para pagar
                entityManager.persist(credito);
            }
        } catch (Exception e) {
            // Si ocurre algún error, lanzar una excepción con el mensaje apropiado
            throw new RuntimeException("Error al procesar la venta: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void descontarStockCaja(Long cajaId, int cantidad) {
        try {
            System.out.println("=== Iniciando descuento de stock ===");
            System.out.println("Caja ID: " + cajaId);
            System.out.println("Cantidad a vender: " + cantidad);

            Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

            System.out.println("Datos de la caja:");
            System.out.println("- Nombre: " + caja.getNombre());
            System.out.println("- Tipo Tasa: " + caja.getTipoTasa());
            System.out.println("- Producto ID: " + caja.getProductoId());
            System.out.println("- Cantidad Unidades: " + caja.getCantidadUnidades());

            // Si la caja tiene un producto asociado, descontar el stock del producto
            if (caja.getProductoId() != null) {
                System.out.println("Buscando producto con ID: " + caja.getProductoId());
                
                // Obtener el producto directamente de la base de datos
                Producto producto = productoRepository.findById(caja.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + caja.getProductoId()));
                
                System.out.println("Datos del producto:");
                System.out.println("- Nombre: " + producto.getNombre());
                System.out.println("- Stock actual: " + producto.getCantidad());
                
                // Obtener la cantidad actual del producto
                Integer cantidadActual = producto.getCantidad();
                if (cantidadActual == null) {
                    throw new RuntimeException("El producto no tiene stock definido");
                }
                
                // Calcular las unidades a descontar (cantidad de cajas * unidades por caja)
                int unidadesADescontar = caja.getCantidadUnidades() * cantidad;
                System.out.println("Cálculo de unidades a descontar:");
                System.out.println("- Unidades por caja: " + caja.getCantidadUnidades());
                System.out.println("- Cantidad de cajas: " + cantidad);
                System.out.println("- Total a descontar: " + unidadesADescontar);
                
                // Verificar si hay suficiente stock
                if (cantidadActual < unidadesADescontar) {
                    throw new RuntimeException(
                        String.format("Stock insuficiente. Disponible: %d, Requerido: %d", 
                            cantidadActual, unidadesADescontar)
                    );
                }
                
                // Descontar el stock y guardar inmediatamente
                System.out.println("Descontando stock...");
                System.out.println("- Stock anterior: " + cantidadActual);
                producto.setCantidad(cantidadActual - unidadesADescontar);
                producto = productoRepository.save(producto);
                System.out.println("- Stock nuevo: " + producto.getCantidad());
                
                // Verificar que el cambio se haya guardado
                if (producto.getCantidad() != (cantidadActual - unidadesADescontar)) {
                    throw new RuntimeException("Error al actualizar el stock del producto");
                }
                
                System.out.println("Stock actualizado correctamente");
            } else {
                System.out.println("La caja no tiene producto asociado");
            }
            
            System.out.println("=== Fin del proceso de descuento ===");
        } catch (Exception e) {
            System.out.println("ERROR en descuento de stock: " + e.getMessage());
            throw new RuntimeException("Error al descontar stock: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VentaCaja> listarVentasPorVenta(Long ventaId) {
        return ventaCajaRepository.findByVentaIdAndActivoTrue(ventaId);
    }

    @Override
    public List<VentaCaja> listarVentasPorCaja(Long cajaId) {
        return ventaCajaRepository.findByCajaIdAndActivoTrue(cajaId);
    }

    @Override
    @Transactional(noRollbackFor = {RuntimeException.class})
    public boolean desactivarVentaCaja(Long ventaCajaId) {
        try {
            VentaCaja ventaCaja = ventaCajaRepository.findById(ventaCajaId)
                .orElseThrow(() -> new RuntimeException("Venta de caja no encontrada: " + ventaCajaId));

            // Desactivar la venta
            ventaCaja.setActivo(false);
            ventaCajaRepository.save(ventaCaja);
            return true;
        } catch (Exception e) {
            // Loguear el error pero permitir que la transacción termine normalmente
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean inactivarVentaCaja(Long id) {
        try {
            // Buscar la venta de caja por su ID
            VentaCaja ventaCaja = ventaCajaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Venta de caja no encontrada con ID: " + id));

            // Cambiar estado a inactivo
            ventaCaja.setActivo(false);

            // Guardar cambios
            ventaCajaRepository.save(ventaCaja);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Optional<VentaCaja> buscarPorId(Long id) {
        return ventaCajaRepository.findById(id);
    }

    @Override
    public List<VentaCajaDTO> buscarVentasCajasPorFechaYTipo(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin, String tipoCaja) {
        // Buscar directamente las ventas de cajas por fecha de creación
        List<VentaCaja> ventasCajas = ventaCajaRepository.findByFechaCreacionBetweenAndActivoTrue(fechaInicio, fechaFin);

        // Filtrar por tipo de caja si es necesario
        if (tipoCaja != null && !tipoCaja.equals("TODAS")) {
            ventasCajas = ventasCajas.stream()
                .filter(vc -> vc.getCaja().getTipo().equals(tipoCaja))
                .toList();
        }

        // Convertir a DTOs para transferir la información
        return ventasCajas.stream()
            .map(vc -> {
                VentaCajaDTO dto = new VentaCajaDTO();
                dto.setId(vc.getId());
                dto.setFechaCreacion(vc.getFechaCreacion());
                dto.setTipoCaja(vc.getCaja().getTipo());
                dto.setCajaNombre(vc.getCaja().getNombre());
                dto.setCantidad(vc.getCantidad());
                dto.setPrecioUnitario(vc.getPrecioUnitario());
                dto.setSubtotal(vc.getSubtotal());

                // Determinar método de pago y cliente según la venta
                if (vc.getVenta() != null) {
                    dto.setMetodoPago(vc.getVenta().getMetodoPago().toString());

                    // Actualizar nombre del cliente según tipo de venta
                    if (vc.getVenta().getTipoVenta() == TipoVenta.CREDITO && vc.getVenta().getCliente() != null) {
                        dto.setNombreCliente(vc.getVenta().getCliente().getNombre());
                    } else {
                        dto.setNombreCliente("Venta al contado");
                    }
                } else {
                    dto.setMetodoPago("EFECTIVO"); // Valor por defecto
                    dto.setNombreCliente("Venta al contado");
                }

                dto.setActivo(vc.isActivo());
                return dto;
            })
            .toList();
    }

    private double obtenerTasaCambio(String tipoTasa) {
        try {
            PrecioDolar.TipoTasa tipo = PrecioDolar.TipoTasa.valueOf(tipoTasa);
            return precioDolarServicio.obtenerPrecioActualPorTipo(
                licoreriaContext.getLicoreriaActual().getId(),
                tipo
            );
        } catch (Exception e) {
            // Si hay algún error, usar la tasa BCV por defecto
            return precioDolarServicio.obtenerPrecioActualPorTipo(
                licoreriaContext.getLicoreriaActual().getId(),
                PrecioDolar.TipoTasa.BCV
            );
        }
    }
} 