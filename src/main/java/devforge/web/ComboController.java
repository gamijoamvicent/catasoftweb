package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Combo;
import devforge.model.ComboProducto;
import devforge.model.Producto;
import devforge.repository.ComboProductoRepository;
import devforge.repository.ComboRepository;
import devforge.repository.ProductoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/combos")
public class ComboController {

    private final ComboRepository comboRepository;
    private final ProductoRepository productoRepository;
    private final ComboProductoRepository comboProductoRepository;
    private final LicoreriaContext licoreriaContext;

    public ComboController(ComboRepository comboRepository, 
                          ProductoRepository productoRepository,
                          ComboProductoRepository comboProductoRepository,
                          LicoreriaContext licoreriaContext) {
        this.comboRepository = comboRepository;
        this.productoRepository = productoRepository;
        this.comboProductoRepository = comboProductoRepository;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/agregar")
    public String mostrarFormularioAgregar(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "combos/agregar";
    }

    @PostMapping("/api/combos")
    @ResponseBody
    public ResponseEntity<?> crearCombo(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            String nombre = (String) payload.get("nombre");
            BigDecimal precio = new BigDecimal(payload.get("precio").toString());
            String tipoTasaStr = (String) payload.get("tipoTasa");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productos = (List<Map<String, Object>>) payload.get("productos");

            Combo combo = new Combo();
            combo.setNombre(nombre);
            combo.setPrecio(precio);
            combo.setTipoTasa(Combo.TipoTasa.valueOf(tipoTasaStr));
            combo.setLicoreria(licoreriaContext.getLicoreriaActual());
            combo.setActivo(true); // Por defecto, los combos nuevos están activos
            combo = comboRepository.save(combo);

            for (Map<String, Object> productoData : productos) {
                Long productoId = ((Number) productoData.get("id")).longValue();
                int cantidad = ((Number) productoData.get("cantidad")).intValue();
                
                // Verificar que el producto pertenezca a la licorería actual
                Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                // Validar que el producto pertenezca a la licorería actual
                if (!producto.getLicoreria().getId().equals(licoreriaContext.getLicoreriaId())) {
                    return ResponseEntity.badRequest()
                        .body("El producto '" + producto.getNombre() + "' no pertenece a la licorería actual");
                }

                ComboProducto comboProducto = new ComboProducto();
                comboProducto.setCombo(combo);
                comboProducto.setProducto(producto);
                comboProducto.setCantidad(cantidad);
                    comboProducto.setLicoreriaId(licoreriaContext.getLicoreriaId()); // Guardar la licorería actual
                comboProductoRepository.save(comboProducto);
            }

            return ResponseEntity.ok(combo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al crear el combo: " + e.getMessage());
        }
    }

    @GetMapping("/api/combos")
    @ResponseBody
    public List<Map<String, Object>> listarCombos() {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return new ArrayList<>();
        }

        Long licoreriaId = licoreriaContext.getLicoreriaId();

        // Usar el método del repositorio que filtra directamente
        return comboRepository.findByLicoreriaIdAndActivoTrue(licoreriaId)
            .stream()
            .map(combo -> {
                Map<String, Object> comboData = new HashMap<>();
                comboData.put("id", combo.getId());
                comboData.put("nombre", combo.getNombre());
                comboData.put("precio", combo.getPrecio());
                comboData.put("tipoTasa", combo.getTipoTasa().toString());
                comboData.put("fechaCreacion", combo.getFechaCreacion());
                comboData.put("activo", combo.getActivo());
                comboData.put("licoreriaId", combo.getLicoreria().getId()); // Agregar el ID de la licorería para verificación
                return comboData;
            })
            .toList();
    }

    @GetMapping("/api/combos/{id}/productos")
    @ResponseBody
    public List<Map<String, Object>> obtenerProductosCombo(
            @PathVariable Long id,
            @RequestParam(required = false) Long licoreriaId) {

        // Si no se proporciona licoreríaId, usar la del contexto actual
        if (licoreriaId == null) {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return List.of();
            }
            licoreriaId = licoreriaContext.getLicoreriaId();
        }

        // Primero verifica que el combo exista
        Combo combo = comboRepository.findById(id).orElse(null);
        if (combo == null) {
            return List.of();
        }

        // Verificar que el combo pertenezca a la licorería solicitada
        if (!combo.getLicoreria().getId().equals(licoreriaId)) {
            return List.of();
        }

        // Primero intentar con el filtro por el campo licoreriaId
        List<ComboProducto> productosCombo = comboProductoRepository.findByComboIdAndLicoreriaId(id, licoreriaId);

        // Si no hay resultados, intentar con el filtro por relación de entidades
        if (productosCombo.isEmpty()) {
            productosCombo = comboProductoRepository.findByComboIdAndComboLicoreriaId(id, licoreriaId);
        }
        return productosCombo
            .stream()
            .map(cp -> {
                Map<String, Object> productoData = new HashMap<>();
                productoData.put("id", cp.getProducto().getId());
                productoData.put("nombre", cp.getProducto().getNombre());
                productoData.put("precioVenta", cp.getProducto().getPrecioVenta());
                productoData.put("cantidad", cp.getCantidad());
                productoData.put("licoreriaId", cp.getLicoreriaId()); // Incluir licoreríaId para verificación
                return productoData;
            })
            .toList();
    }

    @GetMapping("/api/combos/{id}/detalle")
    @ResponseBody
    public ResponseEntity<?> obtenerDetalleCombo(@PathVariable Long id) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
        }

        Combo combo = comboRepository.findById(id).orElse(null);
        if (combo == null) {
            return ResponseEntity.notFound().build();
        }

        // Verificar que el combo pertenezca a la licorería actual
        if (!combo.getLicoreria().getId().equals(licoreriaContext.getLicoreriaId())) {
            return ResponseEntity.badRequest().body("El combo solicitado no pertenece a la licorería actual");
        }

        // Usar el método que filtra por el campo licoreriaId
        List<ComboProducto> productosCombo = comboProductoRepository.findByComboIdAndLicoreriaId(id, licoreriaContext.getLicoreriaId());

        // Si no hay resultados, intentar con el filtro por relación de entidades
        if (productosCombo.isEmpty()) {
            productosCombo = comboProductoRepository.findByComboIdAndComboLicoreriaId(id, licoreriaContext.getLicoreriaId());
        }

        devforge.model.dto.ComboDetalleDTO dto = new devforge.model.dto.ComboDetalleDTO();
        dto.setId(combo.getId());
        dto.setNombre(combo.getNombre());
        dto.setPrecio(combo.getPrecio());

        List<devforge.model.dto.ComboDetalleDTO.ItemDTO> items = new ArrayList<>();
        boolean tieneProductosInactivos = false;
        StringBuilder mensajeActualizacion = new StringBuilder();

        for (ComboProducto cp : productosCombo) {
            devforge.model.dto.ComboDetalleDTO.ItemDTO item = new devforge.model.dto.ComboDetalleDTO.ItemDTO();
            item.setId(cp.getProducto().getId());
            item.setNombre(cp.getProducto().getNombre());
            item.setPrecio(BigDecimal.valueOf(cp.getProducto().getPrecioVenta()));
            item.setCantidad(cp.getCantidad());
            
            // Verificar si el producto está activo
            boolean productoActivo = cp.getProducto().isActivo();
            item.setActivo(productoActivo);
            
            if (!productoActivo) {
                tieneProductosInactivos = true;
                item.setMensajeEstado("Producto inactivo");
                mensajeActualizacion.append("- ").append(cp.getProducto().getNombre()).append(" está inactivo\n");
            }
            
            items.add(item);
        }

        dto.setProductos(items);
        dto.setRequiereActualizacion(tieneProductosInactivos);
        dto.setMensajeActualizacion(tieneProductosInactivos ? 
            "Este combo requiere actualización:\n" + mensajeActualizacion.toString() : 
            "El combo está actualizado y listo para vender");

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/combos/venta")
    @ResponseBody
    public ResponseEntity<?> procesarVentaCombo(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            Long licoreriaId = licoreriaContext.getLicoreriaId();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> combosVendidos = (List<Map<String, Object>>) payload.get("combos");

            for (Map<String, Object> comboVenta : combosVendidos) {
                Long comboId = ((Number) comboVenta.get("id")).longValue();
                int cantidad = ((Number) comboVenta.get("cantidad")).intValue();

                Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new RuntimeException("Combo no encontrado"));

                // Verificar que el combo pertenezca a la licorería actual
                if (!combo.getLicoreria().getId().equals(licoreriaId)) {
                    return ResponseEntity.badRequest()
                        .body("El combo '" + combo.getNombre() + "' no pertenece a la licorería actual");
                }

                List<ComboProducto> productosCombo = comboProductoRepository.findByComboId(comboId);

                for (ComboProducto cp : productosCombo) {
                    Producto producto = cp.getProducto();

                    // Verificar que el producto pertenezca a la licorería actual
                    if (!producto.getLicoreria().getId().equals(licoreriaId)) {
                        return ResponseEntity.badRequest()
                            .body("El producto '" + producto.getNombre() + "' en el combo '" + 
                                  combo.getNombre() + "' no pertenece a la licorería actual");
                    }

                    int cantidadADescontar = cp.getCantidad() * cantidad;
                    
                    if (producto.getCantidad() < cantidadADescontar) {
                        return ResponseEntity.badRequest()
                            .body("No hay suficiente stock de " + producto.getNombre() + 
                                  ". Stock actual: " + producto.getCantidad() + 
                                  ", Se requieren: " + cantidadADescontar);
                    }
                    
                    producto.setCantidad(producto.getCantidad() - cantidadADescontar);
                    productoRepository.save(producto);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Venta procesada exitosamente",
                "fecha", new java.util.Date()
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al procesar la venta: " + e.getMessage());
        }
    }

    @PostMapping("/api/combos/{id}/eliminar")
    @ResponseBody
    public ResponseEntity<?> eliminarCombo(@PathVariable Long id) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Combo no encontrado"));

            if (!combo.getLicoreria().getId().equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.badRequest().body("No tiene permiso para eliminar este combo");
            }

            combo.setActivo(false);
            comboRepository.save(combo);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Combo eliminado exitosamente",
                "fecha", new java.util.Date()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al eliminar el combo: " + e.getMessage());
        }
    }

    @PostMapping("/api/combos/{id}/reactivar")
    @ResponseBody
    public ResponseEntity<?> reactivarCombo(@PathVariable Long id) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Combo no encontrado"));

            if (!combo.getLicoreria().getId().equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.badRequest().body("No tiene permiso para reactivar este combo");
            }

            combo.setActivo(true);
            comboRepository.save(combo);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Combo reactivado exitosamente",
                "fecha", new java.util.Date()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al reactivar el combo: " + e.getMessage());
        }
    }
}