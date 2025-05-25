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

    public NuevaVentaController(
            ProductoServicio productoServicio,
            PrecioDolarServicio precioDolarServicio,
            LicoreriaContext licoreriaContext,
            VentaServicio ventaServicio) {
        this.productoServicio = productoServicio;
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
        this.ventaServicio = ventaServicio;
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        List<Producto> productos = productoServicio.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("precioDolar", precioDolarServicio.obtenerPrecioActual(licoreriaContext.getLicoreriaId()));
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "ventas/nuevaVenta";
    }

    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmarVenta(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Debes seleccionar una licorería primero"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) payload.get("items");
            String metodoPagoStr = (String) payload.get("metodoPago");

            if (itemsMap == null || metodoPagoStr == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Datos de venta incompletos"));
            }

            List<ItemVentaDto> items = itemsMap.stream()
                .map(item -> new ItemVentaDto(
                    Long.valueOf(item.get("id").toString()),
                    Integer.valueOf(item.get("cantidad").toString())
                ))
                .collect(Collectors.toList());

            // Verificar que todos los productos pertenecen a la licorería actual
            List<Producto> productos = new ArrayList<>();
            BigDecimal totalVenta = BigDecimal.ZERO;
            List<DetalleVenta> detalles = new ArrayList<>();

            for (ItemVentaDto item : items) {
                Producto producto = productoServicio.obtenerPorId(item.getId());
                if (producto == null) {
                    return ResponseEntity.status(400)
                        .body(Map.of("error", "Producto no encontrado: " + item.getId()));
                }
                if (!producto.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                    return ResponseEntity.status(403)
                        .body(Map.of("error", "No tienes permiso para vender productos de otra licorería"));
                }
                productos.add(producto);
                
                // Calcular subtotal para este item
                BigDecimal precioUnitario = BigDecimal.valueOf(producto.getPrecioVenta());
                BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
                totalVenta = totalVenta.add(subtotal);
            }

            // Crear la venta
            Venta venta = new Venta();
            venta.setLicoreria(licoreriaContext.getLicoreriaActual());
            venta.setTotalVenta(totalVenta);
            venta.setMetodoPago(Venta.MetodoPago.valueOf(metodoPagoStr));
            venta.setFechaVenta(LocalDateTime.now());

            // Crear los detalles de la venta
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

            // Guardar la venta y sus detalles
            ventaServicio.guardar(venta);

            // Descontar el stock
            productoServicio.descontarStockk(items);

            return ResponseEntity.ok(Map.of("mensaje", "Venta registrada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al procesar la venta: " + e.getMessage()));
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
}
