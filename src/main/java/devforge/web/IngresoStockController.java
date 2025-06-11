package devforge.web;

import devforge.model.Producto;
import devforge.servicio.ProductoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventario")
public class IngresoStockController {

    @Autowired
    private ProductoServicio productoServicio;

    @GetMapping("/ingreso")
    @PreAuthorize("hasAnyRole('BODEGA', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarPaginaIngreso() {
        return "inventario/ingreso";
    }

    @GetMapping("/api/productos")
    @PreAuthorize("hasAnyRole('BODEGA', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
    @ResponseBody
    public ResponseEntity<?> obtenerProductos() {
        try {
            List<Producto> productos = productoServicio.obtenerTodos();
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al obtener productos: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/api/ingreso")
    @PreAuthorize("hasAnyRole('BODEGA', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
    @ResponseBody
    public ResponseEntity<?> registrarIngreso(@RequestBody Map<String, Object> datos) {
        try {
            Long productoId = Long.parseLong(datos.get("productoId").toString());
            Integer cantidad = Integer.parseInt(datos.get("cantidad").toString());
            String motivo = (String) datos.get("motivo");

            productoServicio.registrarIngresoStock(productoId, cantidad, motivo);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Ingreso registrado exitosamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error al registrar el ingreso: " + e.getMessage()
            ));
        }
    }
} 