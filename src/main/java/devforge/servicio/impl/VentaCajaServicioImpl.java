package devforge.servicio.impl;

import devforge.model.Caja;
import devforge.model.Venta;
import devforge.model.VentaCaja;
import devforge.model.Producto;
import devforge.model.enums.MetodoPago;
import devforge.model.enums.TipoVenta;
import devforge.model.PrecioDolar;
import devforge.repository.CajaRepository;
import devforge.repository.VentaCajaRepository;
import devforge.repository.VentaRepository;
import devforge.repository.ProductoRepository;
import devforge.servicio.VentaCajaServicio;
import devforge.config.LicoreriaContext;
import devforge.servicio.PrecioDolarServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class VentaCajaServicioImpl implements VentaCajaServicio {

    private final VentaCajaRepository ventaCajaRepository;
    private final VentaRepository ventaRepository;
    private final CajaRepository cajaRepository;
    private final ProductoRepository productoRepository;
    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;

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
    @Transactional
    public void registrarVenta(List<Map<String, Object>> items) {
        try {
            // Crear la venta
            Venta venta = new Venta();
            venta.setLicoreria(licoreriaContext.getLicoreriaActual());
            venta.setFechaVenta(LocalDateTime.now());
            venta.setTipoVenta(TipoVenta.CONTADO);
            venta.setMetodoPago(MetodoPago.EFECTIVO);
            venta.setAnulada(false);
            venta.setTotalVenta(BigDecimal.ZERO);
            venta.setTotalVentaBs(BigDecimal.ZERO);

            // Guardar la venta primero
            venta = ventaRepository.save(venta);

            // Procesar cada caja
            for (Map<String, Object> item : items) {
                Long cajaId = Long.valueOf(item.get("cajaId").toString());
                int cantidad = Integer.valueOf(item.get("cantidad").toString());
                double precio = Double.valueOf(item.get("precio").toString());
                String tipoTasa = (String) item.get("tipoTasa");

                // Obtener la caja
                Caja caja = cajaRepository.findById(cajaId)
                    .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

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

                // Actualizar totales de la venta
                BigDecimal totalVenta = venta.getTotalVenta() != null ? venta.getTotalVenta() : BigDecimal.ZERO;
                BigDecimal totalVentaBs = venta.getTotalVentaBs() != null ? venta.getTotalVentaBs() : BigDecimal.ZERO;
                
                venta.setTotalVenta(totalVenta.add(ventaCaja.getSubtotal()));
                venta.setTotalVentaBs(totalVentaBs.add(ventaCaja.getSubtotalBolivares()));

                // Guardar el detalle
                ventaCajaRepository.save(ventaCaja);

                // Descontar stock
                descontarStockCaja(cajaId, cantidad);
            }

            // Actualizar la venta con los totales finales
            ventaRepository.save(venta);
        } catch (Exception e) {
            throw new RuntimeException("Error al registrar la venta: " + e.getMessage(), e);
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
        return ventaCajaRepository.findByVentaId(ventaId);
    }

    @Override
    public List<VentaCaja> listarVentasPorCaja(Long cajaId) {
        return ventaCajaRepository.findByCajaId(cajaId);
    }

    private double obtenerTasaCambio(String tipoTasa) {
        return precioDolarServicio.obtenerTasaCambioActual(
            licoreriaContext.getLicoreriaActual().getId()
        );
    }
} 