package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import devforge.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

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
        String username = auth.getName();
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        
        try {
            Licoreria licoreria = licoreriaServicio.obtenerPorId(licoreriaId)
                .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));

            // Verificar que la licorería está activa
            if (!licoreria.isActiva()) {
                logger.warn("Usuario {} intentando seleccionar licorería inactiva: {}", 
                    username, licoreria.getNombre());
                redirectAttrs.addFlashAttribute("error", "La licorería seleccionada está inactiva");
                return "redirect:/licorerias/seleccionar";
            }

            // Verificar permisos de acceso
            if (usuario.getRol() != Usuario.Rol.SUPER_ADMIN && 
                !usuario.getLicoreriaId().equals(licoreria.getId())) {
                logger.warn("Usuario {} intentando acceder a licorería no asignada: {}", 
                    username, licoreria.getNombre());
                return "redirect:/acceso-denegado";
            }

            // Establecer la licorería en el contexto
            licoreriaContext.setLicoreriaActual(licoreria);
            logger.info("Usuario {} seleccionó licorería: {}", username, licoreria.getNombre());
            
            return "redirect:/dashboard";
        } catch (Exception e) {
            logger.error("Error al seleccionar licorería: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("error", "Error al seleccionar la licorería: " + e.getMessage());
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

    // Método para verificar el acceso a las páginas que requieren licorería seleccionada
    private boolean verificarAccesoLicoreria(HttpServletResponse response) throws IOException {
        if (licoreriaContext.getLicoreriaActual() == null) {
            response.sendRedirect("/licorerias/seleccionar");
            return false;
        }
        return true;
    }
}