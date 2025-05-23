package devforge.web;

import devforge.model.PrecioDolar;
import devforge.servicio.PrecioDolarServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracion/dolar")
public class PrecioDolarController {

    @Autowired
    private PrecioDolarServicio servicio;

    //Mostrar formulario
    @GetMapping
    public String mostrarFormulario(Model model) {
        PrecioDolar ultimoPrecio = servicio.obtenerUltimoPrecio();
        model.addAttribute("ultimoPrecio", ultimoPrecio);
        return "configuration/precioDolar";
    }

    @PostMapping
    public String actualizarPrecio(
            @RequestParam double precioDolar,
            RedirectAttributes redirectAttrs) {
        try {
            PrecioDolar nuevoPrecio = new PrecioDolar();
            nuevoPrecio.setPrecioDolar(precioDolar);

            servicio.guardar(nuevoPrecio);
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Precio actualizado correctamente");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al actualizar el precio del dólar");
        }

        return "redirect:/configuracion/dolar";
    }

}
