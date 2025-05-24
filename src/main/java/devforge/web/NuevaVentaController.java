package devforge.web;

import devforge.model.ItemVentaDto;
import devforge.model.Producto;
import devforge.model.ProductoCarritoDto;
import devforge.servicio.ProductoServicio;
import devforge.servicio.PrecioDolarServicio;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas")
public class NuevaVentaController {

    private final ProductoServicio productoServicio;
    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;

    public NuevaVentaController(
            ProductoServicio productoServicio,
            PrecioDolarServicio precioDolarServicio,
            LicoreriaContext licoreriaContext) {
        this.productoServicio = productoServicio;
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        List<Producto> productos = productoServicio.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("precioDolar", precioDolarServicio.obtenerPrecioActual());
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "ventas/nuevaVenta";
    }

    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmarVenta(@RequestBody List<ItemVentaDto> items) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Debes seleccionar una licorería primero"));
            }

            // Verificar que todos los productos pertenecen a la licorería actual
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
            }

            productoServicio.descontarStockk(items);
            return ResponseEntity.ok(Map.of("mensaje", "Venta confirmada, stock actualizado"));
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
