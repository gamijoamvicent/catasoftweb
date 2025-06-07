package devforge.web;

import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/combos")
public class ComboViewController {

    private final LicoreriaContext licoreriaContext;

    public ComboViewController(LicoreriaContext licoreriaContext) {
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/listar")
    public String listarCombos(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "combos/listar";
    }
} 