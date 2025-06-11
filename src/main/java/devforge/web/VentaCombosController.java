package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Combo;
import devforge.servicio.ComboServicio;
import devforge.servicio.VentaComboServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas/combos")
public class VentaCombosController {

    @Autowired
    private ComboServicio comboServicio;

    @Autowired
    private VentaComboServicio ventaComboServicio;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping("/nueva")
    public String nuevaVentaCombo(Model model) {
        if (licoreriaContext.getLicoreriaId() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        List<Combo> combos = comboServicio.listarCombosPorLicoreria(licoreriaContext.getLicoreriaId());
        model.addAttribute("combos", combos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        
        return "ventas/nuevaVentaCombo";
    }

    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmarVenta(@RequestBody Map<String, Object> ventaData) {
        try {
            if (licoreriaContext.getLicoreriaId() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "Debe seleccionar una licorería primero"
                    ));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) ventaData.get("items");
            
            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "No hay items para procesar"
                    ));
            }

            ventaComboServicio.registrarVenta(items);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Venta realizada con éxito",
                "ticketUrl", "/ventas/ticket/" + System.currentTimeMillis()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "Error al procesar la venta: " + e.getMessage()
                ));
        }
    }
} 