package devforge.web;

import devforge.servicio.ImpresoraServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/impresora2")
public class ImpresoraConfigController {
    @Autowired
    private ImpresoraServicio impresoraServicio;

    @GetMapping("/configuracion")
    public String mostrarConfiguracion(Model model) {
        List<String> impresoras = impresoraServicio.listarImpresorasInstaladas();
        model.addAttribute("impresoras", impresoras);
        return "impresora/configuracion2";
    }

    @GetMapping("/listar")
    @ResponseBody
    public List<String> listarImpresoras() {
        return impresoraServicio.listarImpresorasInstaladas();
    }

    @PostMapping("/probar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> probarImpresion(@RequestBody Map<String, String> payload) {
        String impresora = payload.get("impresora");
        String texto = payload.getOrDefault("texto", "Prueba de impresión desde InventSoft");
        try {
            boolean ok = impresoraServicio.imprimirPrueba(impresora, texto);
            if (ok) {
                return ResponseEntity.ok(Map.of("exito", true, "mensaje", "Impresión enviada correctamente a '" + impresora + "'."));
            } else {
                return ResponseEntity.ok(Map.of("exito", false, "mensaje", "No se pudo imprimir en '" + impresora + "'."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("exito", false, "mensaje", e.getMessage()));
        }
    }
}
