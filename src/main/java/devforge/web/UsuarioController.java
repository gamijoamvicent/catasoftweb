package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import devforge.servicio.UsuarioServicio;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioServicio usuarioServicio;
    private final LicoreriaContext licoreriaContext;
    private final LicoreriaServicio licoreriaServicio;

    public UsuarioController(UsuarioServicio usuarioServicio, 
                           LicoreriaContext licoreriaContext,
                           LicoreriaServicio licoreriaServicio) {
        this.usuarioServicio = usuarioServicio;
        this.licoreriaContext = licoreriaContext;
        this.licoreriaServicio = licoreriaServicio;
    }

    @GetMapping("/registrar")
    public String mostrarFormulario(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuarioActual = usuarioServicio.obtenerPorUsername(username);
        
        model.addAttribute("usuarioNuevo", new Usuario());
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());
        model.addAttribute("esSuperAdmin", usuarioActual.getRol() == Usuario.Rol.SUPER_ADMIN);
        model.addAttribute("licoreriaUsuarioActual", usuarioActual.getLicoreria());
        
        if (usuarioActual.getRol() == Usuario.Rol.SUPER_ADMIN) {
            model.addAttribute("licorerias", licoreriaServicio.listarTodas());
        } else {
            model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        }
        
        return "usuarios/registrarUsuarios";
    }

    @PostMapping("/registrar")
    public String registrar(
            @ModelAttribute("usuarioNuevo") Usuario usuario,
            @RequestParam(required = false) Long licoreriaId,
            Authentication auth,
            RedirectAttributes redirectAttrs) {
        try {
            String username = auth.getName();
            Usuario usuarioActual = usuarioServicio.obtenerPorUsername(username);
            
            // Validar campos requeridos
            if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty() ||
                usuario.getPassword() == null || usuario.getPassword().trim().isEmpty() ||
                usuario.getRol() == null) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ Completa todos los campos requeridos");
                return "redirect:/usuarios/registrar";
            }

            // Validar permisos según el rol
            if (usuario.getRol() == Usuario.Rol.ADMIN_LOCAL && 
                usuarioActual.getRol() != Usuario.Rol.SUPER_ADMIN) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ Solo el SUPER_ADMIN puede crear usuarios ADMIN_LOCAL");
                return "redirect:/usuarios/registrar";
            }

            // Asignar licorería según el rol del usuario actual
            if (usuarioActual.getRol() == Usuario.Rol.SUPER_ADMIN) {
                if (licoreriaId == null) {
                    redirectAttrs.addFlashAttribute("mensajeError", "❌ Debes seleccionar una licorería");
                    return "redirect:/usuarios/registrar";
                }
                Licoreria licoreria = licoreriaServicio.obtenerPorId(licoreriaId)
                    .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));
                usuario.setLicoreria(licoreria);
            } else {
                usuario.setLicoreria(licoreriaContext.getLicoreriaActual());
            }

            // Si es una actualización, mantener la contraseña actual
            if (usuario.getId() != null && usuario.getId() > 0) {
                Usuario usuarioExistente = usuarioServicio.obtenerPorId(usuario.getId());
                usuario.setPassword(usuarioExistente.getPassword());
            }

            usuarioServicio.guardar(usuario);
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Usuario registrado exitosamente");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al guardar el usuario: " + e.getMessage());
        }

        return "redirect:/usuarios/registrar";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttrs) {
        try {
            String username = auth.getName();
            Usuario usuarioActual = usuarioServicio.obtenerPorUsername(username);
            Usuario usuarioAEliminar = usuarioServicio.obtenerPorId(id);

            // Validar permisos
            if (usuarioAEliminar.getRol() == Usuario.Rol.SUPER_ADMIN) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ No se puede eliminar un SUPER_ADMIN");
                return "redirect:/usuarios/registrar";
            }

            if (usuarioAEliminar.getRol() == Usuario.Rol.ADMIN_LOCAL && 
                usuarioActual.getRol() != Usuario.Rol.SUPER_ADMIN) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ Solo el SUPER_ADMIN puede eliminar usuarios ADMIN_LOCAL");
                return "redirect:/usuarios/registrar";
            }

            // Validar que el usuario pertenezca a la misma licorería o sea super admin
            if (usuarioActual.getRol() != Usuario.Rol.SUPER_ADMIN && 
                (usuarioAEliminar.getLicoreria() == null || 
                 !usuarioAEliminar.getLicoreria().getId().equals(usuarioActual.getLicoreria().getId()))) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ No tienes permisos para eliminar usuarios de otras licorerías");
                return "redirect:/usuarios/registrar";
            }

            usuarioServicio.eliminar(id);
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Usuario eliminado exitosamente");

        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al eliminar el usuario: " + e.getMessage());
        }

        return "redirect:/usuarios/registrar";
    }

    @GetMapping("/editar/{id}")
    public String mostrarEdicion(@PathVariable Long id, Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuarioActual = usuarioServicio.obtenerPorUsername(username);
        Usuario usuario = usuarioServicio.obtenerPorId(id);

        // Validar permisos
        if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN && 
            usuarioActual.getRol() != Usuario.Rol.SUPER_ADMIN) {
            return "redirect:/usuarios/registrar";
        }

        // Validar que el usuario pertenezca a la misma licorería o sea super admin
        if (usuarioActual.getRol() != Usuario.Rol.SUPER_ADMIN && 
            (usuario.getLicoreria() == null || 
             !usuario.getLicoreria().getId().equals(usuarioActual.getLicoreria().getId()))) {
            return "redirect:/usuarios/registrar";
        }
        
        model.addAttribute("usuarioNuevo", usuario);
        model.addAttribute("usuarios", usuarioServicio.listarUsuarios());
        model.addAttribute("esSuperAdmin", usuarioActual.getRol() == Usuario.Rol.SUPER_ADMIN);
        model.addAttribute("licoreriaUsuarioActual", usuarioActual.getLicoreria());
        
        if (usuarioActual.getRol() == Usuario.Rol.SUPER_ADMIN) {
            model.addAttribute("licorerias", licoreriaServicio.listarTodas());
        } else {
            model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        }
        
        return "usuarios/registrarUsuarios";
    }
}