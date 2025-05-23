package devforge.web;

import devforge.model.Usuario;
import devforge.servicio.UsuarioServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioServicio usuarioServicio;

    public UsuarioController(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    // Mostrar formulario de registro (solo ADMIN)
    @GetMapping("/registrar")
    public String mostrarFormulario(Model model) {
        model.addAttribute("usuarioNuevo", new Usuario());
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());
        return "usuarios/registrarUsuarios";
    }

    // Guardar nuevo usuario o actualizar uno existente
    @PostMapping("/registrar")
    public String registrar(@ModelAttribute("usuarioNuevo") Usuario usuario, Model model) {
        try {
            if (usuario.getUsername() == null || usuario.getRol() == null) {
                model.addAttribute("mensajeError", "❌ Completa todos los campos");
                return "usuarios/registrarUsuarios";
            }

            // ✅ Si tiene ID → es una actualización
            if (usuario.getId() != null && usuario.getId() > 0) {
                Usuario usuarioExistente = usuarioServicio.obtenerPorId(usuario.getId());
                usuario.setPassword(usuarioExistente.getPassword()); // No cambia la contraseña si ya existe
            } else {
                usuario.setPassword(usuario.getPassword()); // Si es nuevo → se codifica
            }

            usuarioServicio.guardar(usuario);
            model.addAttribute("mensajeExito", "✅ Usuario registrado exitosamente");

        } catch (Exception e) {
            model.addAttribute("mensajeError", "❌ Error al guardar el usuario");
        }

        return "redirect:/usuarios/registrar";
    }

    // Eliminar usuario
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        usuarioServicio.eliminar(id);
        return "redirect:/usuarios/registrar";
    }

    // Editar usuario
    @GetMapping("/editar/{id}")
    public String mostrarEdicion(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioServicio.obtenerPorId(id);
        model.addAttribute("usuarioNuevo", usuario);
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());
        return "usuarios/registrarUsuarios";
    }
}