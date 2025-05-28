package devforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaletaController {

    @GetMapping("/paletas")
    public String mostrarPaletas() {
        return "paletas";
    }
} 