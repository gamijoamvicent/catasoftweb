package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Caja;
import devforge.model.Licoreria;
import devforge.model.Producto;
import devforge.servicio.CajaServicio;
import devforge.servicio.ProductoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cajas")
public class CajaController {

    private final CajaServicio cajaServicio;
    private final ProductoServicio productoServicio;
    private final LicoreriaContext licoreriaContext;

    @Autowired
    public CajaController(
            CajaServicio cajaServicio,
            ProductoServicio productoServicio,
            LicoreriaContext licoreriaContext) {
        this.cajaServicio = cajaServicio;
        this.productoServicio = productoServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/configuracion")
    public String mostrarConfiguracionCajas(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "cajas/configuracion";
    }

    @GetMapping("/api/cajas")
    @ResponseBody
    public List<Map<String, Object>> listarCajas() {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return new ArrayList<>();
        }

        List<Caja> cajas = cajaServicio.listarActivosPorLicoreria(licoreriaContext.getLicoreriaId());
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Caja caja : cajas) {
            Map<String, Object> cajaData = new HashMap<>();
            cajaData.put("id", caja.getId());
            cajaData.put("nombre", caja.getNombre());
            cajaData.put("descripcion", caja.getDescripcion());
            cajaData.put("precio", caja.getPrecio());
            cajaData.put("cantidadUnidades", caja.getCantidadUnidades());
            cajaData.put("productoId", caja.getProductoId());
            cajaData.put("productoNombre", caja.getProducto().getNombre());
            cajaData.put("tipoTasa", caja.getTipoTasa());
            cajaData.put("fechaCreacion", caja.getFechaCreacion());
            cajaData.put("estado", caja.getEstado());
            resultado.add(cajaData);
        }

        return resultado;
    }

    @GetMapping("/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarCajas(@RequestParam String termino) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return new ArrayList<>();
        }

        List<Caja> cajas = cajaServicio.buscarPorNombre(termino, licoreriaContext.getLicoreriaId());
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Caja caja : cajas) {
            Map<String, Object> cajaData = new HashMap<>();
            cajaData.put("id", caja.getId());
            cajaData.put("nombre", caja.getNombre());
            cajaData.put("precio", caja.getPrecio());
            cajaData.put("cantidadUnidades", caja.getCantidadUnidades());
            cajaData.put("productoNombre", caja.getProducto().getNombre());
            resultado.add(cajaData);
        }

        return resultado;
    }

    @PostMapping("/api/cajas")
    @ResponseBody
    public ResponseEntity<?> crearCaja(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            // Validar que los campos requeridos existan en el payload
            if (!payload.containsKey("nombre") || !payload.containsKey("precio") || 
                !payload.containsKey("cantidadUnidades") || !payload.containsKey("productoId") || 
                !payload.containsKey("tipoTasa")) {
                return ResponseEntity.badRequest().body("Faltan campos requeridos");
            }

            String nombre = (String) payload.get("nombre");
            String descripcion = (String) payload.get("descripcion");

            // Validar y convertir valores numéricos con manejo específico de errores
            double precio;
            int cantidadUnidades;
            Long productoId;
            try {
                precio = Double.parseDouble(payload.get("precio").toString());
                cantidadUnidades = Integer.parseInt(payload.get("cantidadUnidades").toString());
                productoId = Long.parseLong(payload.get("productoId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Error en formato de datos numéricos: " + e.getMessage());
            }

            String tipoTasa = (String) payload.get("tipoTasa");

            Producto producto = productoServicio.obtenerPorId(productoId);
            if (producto == null) {
                return ResponseEntity.badRequest().body("Producto no encontrado con ID: " + productoId);
            }

            Licoreria licoreria = licoreriaContext.getLicoreriaActual();

            Caja caja = new Caja();
            caja.setNombre(nombre);
            caja.setDescripcion(descripcion);
            caja.setPrecio(precio);
            caja.setCantidadUnidades(cantidadUnidades);
            caja.setProducto(producto);
            caja.setLicoreria(licoreria);
            caja.setTipoTasa(tipoTasa);
            caja.setEstado(true);

            Caja cajaGuardada = cajaServicio.guardar(caja);
            if (cajaGuardada == null || cajaGuardada.getId() == null) {
                return ResponseEntity.status(500).body("Error al guardar la caja en la base de datos");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", cajaGuardada.getId());
            response.put("mensaje", "Caja creada exitosamente");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // Log detallado para diagnóstico
            return ResponseEntity.status(500).body("Error al crear la caja: " + e.getMessage());
        }
    }

    @PutMapping("/api/cajas/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarCaja(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Caja caja = cajaServicio.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

            if (!caja.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.badRequest().body("No tiene permiso para modificar esta caja");
            }

            // Validar que los campos requeridos existan en el payload
            if (!payload.containsKey("nombre") || !payload.containsKey("precio") || 
                !payload.containsKey("cantidadUnidades") || !payload.containsKey("productoId") || 
                !payload.containsKey("tipoTasa")) {
                return ResponseEntity.badRequest().body("Faltan campos requeridos");
            }

            String nombre = (String) payload.get("nombre");
            String descripcion = (String) payload.get("descripcion");

            // Validar y convertir valores numéricos con manejo específico de errores
            double precio;
            int cantidadUnidades;
            Long productoId;
            try {
                precio = Double.parseDouble(payload.get("precio").toString());
                cantidadUnidades = Integer.parseInt(payload.get("cantidadUnidades").toString());
                productoId = Long.parseLong(payload.get("productoId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("Error en formato de datos numéricos: " + e.getMessage());
            }

            String tipoTasa = (String) payload.get("tipoTasa");

            Producto producto = productoServicio.obtenerPorId(productoId);
            if (producto == null) {
                return ResponseEntity.badRequest().body("Producto no encontrado con ID: " + productoId);
            }

            caja.setNombre(nombre);
            caja.setDescripcion(descripcion);
            caja.setPrecio(precio);
            caja.setCantidadUnidades(cantidadUnidades);
            caja.setProducto(producto);
            caja.setTipoTasa(tipoTasa);

            Caja cajaGuardada = cajaServicio.guardar(caja);
            if (cajaGuardada == null) {
                return ResponseEntity.status(500).body("Error al guardar la caja en la base de datos");
            }

            return ResponseEntity.ok(Map.of("mensaje", "Caja actualizada exitosamente"));

        } catch (Exception e) {
            e.printStackTrace(); // Log detallado para diagnóstico
            return ResponseEntity.status(500).body("Error al actualizar la caja: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/cajas/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarCaja(@PathVariable Long id) {
        try {
            Caja caja = cajaServicio.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

            if (!caja.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.badRequest().body("No tiene permiso para eliminar esta caja");
            }

            // Cambiar estado en lugar de eliminar físicamente
            cajaServicio.cambiarEstado(id, false);

            return ResponseEntity.ok(Map.of("mensaje", "Caja eliminada exitosamente"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar la caja: " + e.getMessage());
        }
    }

    @PostMapping("/api/cajas/{id}/reactivar")
    @ResponseBody
    public ResponseEntity<?> reactivarCaja(@PathVariable Long id) {
        try {
            Caja caja = cajaServicio.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

            if (!caja.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.badRequest().body("No tiene permiso para reactivar esta caja");
            }

            cajaServicio.cambiarEstado(id, true);

            return ResponseEntity.ok(Map.of("mensaje", "Caja reactivada exitosamente"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al reactivar la caja: " + e.getMessage());
        }
    }
}
