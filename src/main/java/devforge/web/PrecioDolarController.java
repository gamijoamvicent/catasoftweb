package devforge.web;

import devforge.model.PrecioDolar;
import devforge.servicio.PrecioDolarServicio;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

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
        
        List<PrecioDolar> tasasActuales = servicio.obtenerUltimasTasas(licoreriaContext.getLicoreriaId());
        model.addAttribute("tasasActuales", tasasActuales);
        return "dolar/actualizar";
    }

    @PostMapping("/actualizar")
    public String actualizarPrecio(
            @RequestParam double precioDolar,
            @RequestParam PrecioDolar.TipoTasa tipoTasa,
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

            if (tipoTasa == null) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ El tipo de tasa es inválido");
                return "redirect:/dolar/actualizar";
            }

            PrecioDolar nuevoPrecio = new PrecioDolar();
            nuevoPrecio.setPrecioDolar(precioDolar);
            nuevoPrecio.setTipoTasa(tipoTasa);
            nuevoPrecio.setLicoreria(licoreriaContext.getLicoreriaActual());
            nuevoPrecio.setFechaDolar(new Date());
            nuevoPrecio.setFechaCreacion(LocalDateTime.now());
            
            servicio.guardar(nuevoPrecio);
            
            redirectAttrs.addFlashAttribute("mensajeExito", 
                "✅ " + tipoTasa + " actualizada a " + precioDolar + " Bs");
            return "redirect:/dolar/actualizar";

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al actualizar el precio del dólar: " + e.getMessage());
            return "redirect:/dolar/actualizar";
        }
    }
}
