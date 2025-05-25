package devforge.web;

import devforge.model.*;
import devforge.servicio.*;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas")
public class NuevaVentaController {

    private final ProductoServicio productoServicio;
    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;
    private final VentaServicio ventaServicio;
    private final ClienteServicio clienteServicio;
    private final CreditoServicio creditoServicio;

    public NuevaVentaController(
            ProductoServicio productoServicio,
            PrecioDolarServicio precioDolarServicio,
            LicoreriaContext licoreriaContext,
            VentaServicio ventaServicio,
            ClienteServicio clienteServicio,
            CreditoServicio creditoServicio) {
        this.productoServicio = productoServicio;
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
        this.ventaServicio = ventaServicio;
        this.clienteServicio = clienteServicio;
        this.creditoServicio = creditoServicio;
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevaVenta(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        Double precioDolar = precioDolarServicio.obtenerPrecioActual(licoreriaContext.getLicoreriaId());
        List<Producto> productos = productoServicio.listarProductosPorLicoreria(licoreriaContext.getLicoreriaId());
        
        model.addAttribute("precioDolar", precioDolar);
        model.addAttribute("productos", productos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        
        return "ventas/nuevaVenta";
    }

    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmarVenta(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) payload.get("items");
            String metodoPagoStr = (String) payload.get("metodoPago");
            String tipoVentaStr = (String) payload.get("tipoVenta");
            Long clienteId = payload.get("clienteId") != null ? 
                Long.valueOf(payload.get("clienteId").toString()) : null;

            // Validaciones básicas
            if (itemsMap == null || itemsMap.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe agregar al menos un producto"));
            }

            if (metodoPagoStr == null || tipoVentaStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos de venta incompletos"));
            }

            Venta.TipoVenta tipoVenta = Venta.TipoVenta.valueOf(tipoVentaStr);
            
            // Validación de cliente para venta a crédito
            Cliente cliente = null;
            if (tipoVenta == Venta.TipoVenta.CREDITO) {
                if (clienteId == null) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe seleccionar un cliente para ventas a crédito"));
                }
                
                cliente = clienteServicio.buscarPorId(clienteId)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
                
                if (!cliente.isEstado()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "El cliente está inactivo"));
                }

                if (!cliente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "El cliente no pertenece a esta licorería"));
                }
            }

            // Procesar items y validar stock
            List<ItemVentaDto> items = new ArrayList<>();
            List<Producto> productos = new ArrayList<>();
            BigDecimal totalVenta = BigDecimal.ZERO;

            for (Map<String, Object> item : itemsMap) {
                Long productoId = Long.valueOf(item.get("id").toString());
                Integer cantidad = Integer.valueOf(item.get("cantidad").toString());

                Producto producto = productoServicio.obtenerPorId(productoId);
                if (producto == null) {
                    throw new RuntimeException("Producto no encontrado: " + productoId);
                }

                if (!producto.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                    throw new RuntimeException("El producto no pertenece a esta licorería");
                }

                if (producto.getCantidad() < cantidad) {
                    throw new RuntimeException(
                        String.format("Stock insuficiente para %s. Disponible: %d, Solicitado: %d",
                            producto.getNombre(), producto.getCantidad(), cantidad));
                }

                ItemVentaDto itemDto = new ItemVentaDto(productoId, cantidad);
                items.add(itemDto);
                productos.add(producto);

                BigDecimal subtotal = BigDecimal.valueOf(producto.getPrecioVenta())
                    .multiply(BigDecimal.valueOf(cantidad));
                totalVenta = totalVenta.add(subtotal);
            }

            // Validar límite de crédito si es venta a crédito
            if (tipoVenta == Venta.TipoVenta.CREDITO) {
                if (totalVenta.doubleValue() > cliente.getCreditoDisponible()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", 
                            String.format("El monto de la venta ($%.2f) excede el crédito disponible ($%.2f)",
                                totalVenta.doubleValue(), cliente.getCreditoDisponible())));
                }
            }

            // Crear la venta
            Venta venta = new Venta();
            List<DetalleVenta> detalles = new ArrayList<>();
            
            venta.setLicoreria(licoreriaContext.getLicoreriaActual());
            venta.setTotalVenta(totalVenta);
            venta.setMetodoPago(Venta.MetodoPago.valueOf(metodoPagoStr));
            venta.setTipoVenta(tipoVenta);
            venta.setCliente(cliente);
            venta.setFechaVenta(LocalDateTime.now());

            // Crear los detalles
            for (int i = 0; i < items.size(); i++) {
                ItemVentaDto item = items.get(i);
                Producto producto = productos.get(i);
                
                DetalleVenta detalle = new DetalleVenta();
                detalle.setVenta(venta);
                detalle.setProducto(producto);
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(BigDecimal.valueOf(producto.getPrecioVenta()));
                detalle.setSubtotal(BigDecimal.valueOf(producto.getPrecioVenta())
                    .multiply(BigDecimal.valueOf(item.getCantidad())));
                
                detalles.add(detalle);
            }
            venta.setDetalles(detalles);

            // Guardar la venta
            ventaServicio.guardar(venta);

            // Si es venta a crédito, crear el crédito
            if (tipoVenta == Venta.TipoVenta.CREDITO) {
                creditoServicio.crearCredito(venta);
            }

            // Descontar el stock
            for (ItemVentaDto item : items) {
                productoServicio.descontarStock(item.getId(), item.getCantidad());
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "✅ Venta registrada exitosamente",
                "ventaId", venta.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "❌ Error al procesar la venta: " + e.getMessage()));
        }
    }

    @GetMapping("/productos")
    @ResponseBody
    public ResponseEntity<?> obtenerProductos() {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Debes seleccionar una licorería primero"));
            }
            List<Producto> productos = productoServicio.listarProductos().stream()
                .filter(p -> p.getLicoreriaId().equals(licoreriaContext.getLicoreriaId()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al obtener productos: " + e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<?> buscarProductos(@RequestParam String termino) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Debes seleccionar una licorería primero"));
            }
            List<Producto> productos = productoServicio.buscarPorNombreOCodigo(termino).stream()
                .filter(p -> p.getLicoreriaId().equals(licoreriaContext.getLicoreriaId()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al buscar productos: " + e.getMessage()));
        }
    }

    @GetMapping("/detalle/{id}")
    public String verDetalleVenta(@PathVariable Long id, Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        Venta venta = ventaServicio.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (!venta.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
            throw new RuntimeException("No tienes permiso para ver esta venta");
        }

        model.addAttribute("venta", venta);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());

        return "ventas/detalle";
    }

    private static class ItemVentaDto {
        private Long id;
        private Integer cantidad;

        public ItemVentaDto(Long id, Integer cantidad) {
            this.id = id;
            this.cantidad = cantidad;
        }

        public Long getId() {
            return id;
        }

        public Integer getCantidad() {
            return cantidad;
        }
    }
}
