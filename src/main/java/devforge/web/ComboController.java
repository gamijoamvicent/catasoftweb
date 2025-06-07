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
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productos = (List<Map<String, Object>>) payload.get("productos");

            Combo combo = new Combo();
            combo.setNombre(nombre);
            combo.setPrecio(precio);
            combo.setLicoreria(licoreriaContext.getLicoreriaActual());
            combo = comboRepository.save(combo);

            for (Map<String, Object> productoData : productos) {
                Long productoId = ((Number) productoData.get("id")).longValue();
                int cantidad = ((Number) productoData.get("cantidad")).intValue();
                
                Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                ComboProducto comboProducto = new ComboProducto();
                comboProducto.setCombo(combo);
                comboProducto.setProducto(producto);
                comboProducto.setCantidad(cantidad);
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

        return comboRepository.findAll()
            .stream()
            .filter(combo -> combo.getLicoreria().getId().equals(licoreriaContext.getLicoreriaId()))
            .map(combo -> {
                Map<String, Object> comboData = new HashMap<>();
                comboData.put("id", combo.getId());
                comboData.put("nombre", combo.getNombre());
                comboData.put("precio", combo.getPrecio());
                comboData.put("fechaCreacion", combo.getFechaCreacion());
                return comboData;
            })
            .toList();
    }

    @GetMapping("/api/combos/{id}/productos")
    @ResponseBody
    public List<Map<String, Object>> obtenerProductosCombo(@PathVariable Long id) {
        List<ComboProducto> productosCombo = comboProductoRepository.findByComboId(id);
        System.out.println("[DEBUG] Productos encontrados para combo " + id + ": " + productosCombo.size());
        return productosCombo
            .stream()
            .map(cp -> {
                Map<String, Object> productoData = new HashMap<>();
                productoData.put("id", cp.getProducto().getId());
                productoData.put("nombre", cp.getProducto().getNombre());
                productoData.put("precioVenta", cp.getProducto().getPrecioVenta());
                productoData.put("cantidad", cp.getCantidad());
                return productoData;
            })
            .toList();
    }

    @GetMapping("/api/combos/{id}/detalle")
    @ResponseBody
    public ResponseEntity<?> obtenerDetalleCombo(@PathVariable Long id) {
        Combo combo = comboRepository.findById(id).orElse(null);
        if (combo == null) {
            return ResponseEntity.notFound().build();
        }
        List<ComboProducto> productosCombo = comboProductoRepository.findByComboId(id);
        devforge.model.dto.ComboDetalleDTO dto = new devforge.model.dto.ComboDetalleDTO();
        dto.setId(combo.getId());
        dto.setNombre(combo.getNombre());
        dto.setPrecio(combo.getPrecio());
        List<devforge.model.dto.ComboDetalleDTO.ItemDTO> items = new ArrayList<>();
        for (ComboProducto cp : productosCombo) {
            devforge.model.dto.ComboDetalleDTO.ItemDTO item = new devforge.model.dto.ComboDetalleDTO.ItemDTO();
            item.setId(cp.getProducto().getId());
            item.setNombre(cp.getProducto().getNombre());
            item.setPrecio(BigDecimal.valueOf(cp.getProducto().getPrecioVenta()));
            item.setCantidad(cp.getCantidad());
            items.add(item);
        }
        dto.setProductos(items);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/combos/venta")
    @ResponseBody
    public ResponseEntity<?> procesarVentaCombo(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> combosVendidos = (List<Map<String, Object>>) payload.get("combos");
            
            for (Map<String, Object> comboVenta : combosVendidos) {
                Long comboId = ((Number) comboVenta.get("id")).longValue();
                int cantidad = ((Number) comboVenta.get("cantidad")).intValue();
                
                Combo combo = comboRepository.findById(comboId)
                    .orElseThrow(() -> new RuntimeException("Combo no encontrado"));
                
                List<ComboProducto> productosCombo = comboProductoRepository.findByComboId(comboId);
                
                for (ComboProducto cp : productosCombo) {
                    Producto producto = cp.getProducto();
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
}