package devforge.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login"; // Apunta a src/main/resources/templates/login.html
    }

    @GetMapping("/logout")
    public String cerrarSesion() {
        return "redirect:/login";
    }
}