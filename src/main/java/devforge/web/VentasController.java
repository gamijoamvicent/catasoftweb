package devforge.web;

import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/ventas")
public class VentasController {

    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarPaginaVentas(Model model) {
        // Verificar si hay una licorería seleccionada
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        // Agregar datos necesarios al modelo
        model.addAttribute("licoreria", licoreriaContext.getLicoreriaActual());

        return "ventas/index";
    }
}
