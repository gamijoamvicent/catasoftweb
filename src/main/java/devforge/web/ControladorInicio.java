package devforge.web;

import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorInicio {

    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping("/")
    public String inicio() {
        return "redirect:/dashboard";
    }
}