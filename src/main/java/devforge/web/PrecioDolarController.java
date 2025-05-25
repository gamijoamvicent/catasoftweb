package devforge.web;

import devforge.model.PrecioDolar;
import devforge.servicio.PrecioDolarServicio;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Date;

@Controller
@RequestMapping("/dolar")
public class PrecioDolarController {

    private final PrecioDolarServicio servicio;
    private final LicoreriaContext licoreriaContext;

    public PrecioDolarController(PrecioDolarServicio servicio, LicoreriaContext licoreriaContext) {
        this.servicio = servicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/actualizar")
    public String mostrarFormulario(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        PrecioDolar ultimoPrecio = servicio.obtenerUltimoPrecio(licoreriaContext.getLicoreriaId());
        model.addAttribute("ultimoPrecio", ultimoPrecio);
        return "dolar/actualizar";
    }

    @PostMapping("/actualizar")
    public String actualizarPrecio(
            @RequestParam double precioDolar,
            RedirectAttributes redirectAttrs) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ Debes seleccionar una licorería primero");
                return "redirect:/licorerias/seleccionar";
            }

            if (precioDolar <= 0) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ El precio del dólar debe ser mayor a 0");
                return "redirect:/dolar/actualizar";
            }

            PrecioDolar nuevoPrecio = new PrecioDolar();
            nuevoPrecio.setPrecioDolar(precioDolar);
            nuevoPrecio.setLicoreria(licoreriaContext.getLicoreriaActual());
            nuevoPrecio.setFechaDolar(new Date());
            servicio.guardar(nuevoPrecio);
            
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Precio del dólar actualizado a " + precioDolar + " Bs");
            return "redirect:/";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al actualizar el precio del dólar: " + e.getMessage());
            return "redirect:/dolar/actualizar";
        }
    }
}
