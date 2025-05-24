package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import devforge.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/licorerias")
public class LicoreriaController {
    private static final Logger logger = LoggerFactory.getLogger(LicoreriaController.class);

    @Autowired
    private LicoreriaServicio licoreriaServicio;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private UsuarioServicio usuarioServicio;

    // Vista de selección de licorería
    @GetMapping("/seleccionar")
    public String mostrarSeleccionLicoreria(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        logger.info("Mostrando selección de licorería para usuario: {}", username);
        
        if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN) {
            model.addAttribute("licorerias", licoreriaServicio.listarTodas());
        } else {
            model.addAttribute("licorerias", licoreriaServicio.listarActivas());
        }
        
        return "licorerias/seleccionar";
    }

    // Procesar selección de licorería
    @PostMapping("/seleccionar")
    public String seleccionarLicoreria(@RequestParam Long licoreriaId, 
                                     RedirectAttributes redirectAttrs, 
                                     Authentication auth) {
        try {
            String username = auth.getName();
            logger.info("Usuario {} intentando seleccionar licorería ID: {}", username, licoreriaId);
            
            Licoreria licoreria = licoreriaServicio.obtenerPorId(licoreriaId)
                .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));
            
            if (!licoreria.isEstado()) {
                logger.warn("Intento de seleccionar licorería inactiva ID: {}", licoreriaId);
                redirectAttrs.addFlashAttribute("mensajeError", "❌ No se puede seleccionar una licorería inactiva");
                return "redirect:/licorerias/seleccionar";
            }
            
            Usuario usuario = usuarioServicio.obtenerPorUsername(username);
            if (usuario.getRol() != Usuario.Rol.SUPER_ADMIN && 
                !usuario.getLicoreriaId().equals(licoreriaId)) {
                logger.warn("Usuario {} intentando seleccionar licorería no asignada", username);
                redirectAttrs.addFlashAttribute("mensajeError", "❌ No tienes permiso para seleccionar esta licorería");
                return "redirect:/licorerias/seleccionar";
            }
            
            // Establecer la licorería en el contexto
            licoreriaContext.setLicoreriaActual(licoreria);
            
            logger.info("Licorería {} (ID: {}) seleccionada exitosamente para usuario {}", 
                licoreria.getNombre(), licoreria.getId(), username);
            
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Licorería seleccionada: " + licoreria.getNombre());
            return "redirect:/dashboard";
        } catch (Exception e) {
            logger.error("Error al seleccionar licorería: {}", e.getMessage(), e);
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al seleccionar la licorería: " + e.getMessage());
            return "redirect:/licorerias/seleccionar";
        }
    }

    // Vista de gestión de licorerías (solo SUPER_ADMIN)
    @GetMapping("/gestionar")
    public String gestionarLicorerias(Model model) {
        model.addAttribute("licorerias", licoreriaServicio.listarTodas());
        model.addAttribute("nuevaLicoreria", new Licoreria());
        return "licorerias/gestionar";
    }

    // Guardar nueva licorería
    @PostMapping("/guardar")
    public String guardarLicoreria(@ModelAttribute Licoreria licoreria, RedirectAttributes redirectAttrs) {
        try {
            licoreriaServicio.guardar(licoreria);
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Licorería guardada exitosamente");
            return "redirect:/licorerias/seleccionar";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al guardar la licorería");
            return "redirect:/licorerias/gestionar";
        }
    }

    // Cambiar estado de licorería
    @PostMapping("/{id}/cambiar-estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean estado, RedirectAttributes redirectAttributes) {
        try {
            licoreriaServicio.cambiarEstado(id, estado);
            String mensaje = estado ? "Licorería activada exitosamente" : "Licorería desactivada exitosamente";
            redirectAttributes.addFlashAttribute("mensaje", mensaje);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar el estado de la licorería");
        }
        return "redirect:/licorerias";
    }

    // Eliminar licorería
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public String eliminarLicoreria(@PathVariable Long id) {
        try {
            licoreriaServicio.eliminar(id);
            return "OK";
        } catch (Exception e) {
            return "ERROR";
        }
    }
} 