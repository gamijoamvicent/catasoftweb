package devforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/combos")
public class ComboViewController {

    @GetMapping("/agregar")
    public String mostrarFormularioAgregar() {
        return "combos/agregar";
    }
} 