package devforge.web;

import devforge.model.Combo;
import devforge.model.ComboProducto;
import devforge.model.Producto;
import devforge.model.dto.ComboDTO;
import devforge.repository.ComboRepository;
import devforge.repository.ComboProductoRepository;
import devforge.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/combos")
@CrossOrigin(origins = "*")
public class ComboController {

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private ComboProductoRepository comboProductoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @PostMapping
    public ResponseEntity<Combo> crearCombo(@RequestBody ComboDTO comboDTO) {
        try {
            // Crear el combo
            Combo combo = new Combo();
            combo.setNombre(comboDTO.getNombre());
            combo.setPrecio(comboDTO.getPrecio());
            Combo comboGuardado = comboRepository.save(combo);

            // Guardar los productos del combo
            if (comboDTO.getProductoIds() != null) {
                for (Long productoId : comboDTO.getProductoIds()) {
                    Producto producto = productoRepository.findById(productoId)
                            .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productoId));
                    
                    ComboProducto comboProducto = new ComboProducto();
                    comboProducto.setCombo(comboGuardado);
                    comboProducto.setProducto(producto);
                    comboProductoRepository.save(comboProducto);
                }
            }

            return ResponseEntity.ok(comboGuardado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Combo>> listarCombos() {
        try {
            List<Combo> combos = comboRepository.findAll();
            return ResponseEntity.ok(combos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Combo> obtenerCombo(@PathVariable Long id) {
        try {
            return comboRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<Producto>> obtenerProductosDelCombo(@PathVariable Long id) {
        try {
            List<ComboProducto> comboProductos = comboProductoRepository.findByComboId(id);
            List<Producto> productos = comboProductos.stream()
                    .map(ComboProducto::getProducto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 