package devforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorController {

    @GetMapping("/error")
    public String mostrarError(@RequestParam(required = false) String mensaje, Model model) {
        model.addAttribute("mensajeError", mensaje != null ? mensaje : "Ha ocurrido un error inesperado");
        return "error/error";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "error/accesoDenegado";
    }
} 