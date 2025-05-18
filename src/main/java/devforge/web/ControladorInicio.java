package devforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorInicio {

    @GetMapping("/inicio")
    public String inicio(Model model) {
        model.addAttribute("titulo", "Bienvenido al Sistema");
        return "index"; // Esto apunta a /templates/index.html
    }
}