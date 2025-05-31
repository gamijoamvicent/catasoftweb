package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import devforge.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        try {
            String username = auth.getName();
            Usuario usuario = usuarioServicio.obtenerPorUsername(username);
            logger.info("Mostrando selección de licorería para usuario: {}", username);
            
            if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN) {
                model.addAttribute("licorerias", licoreriaServicio.listarTodas());
            } else {
                model.addAttribute("licorerias", licoreriaServicio.listarActivas());
            }
            
            return "licorerias/seleccionar";
        } catch (Exception e) {
            logger.error("Error al mostrar selección de licorería: {}", e.getMessage(), e);
            return "redirect:/error?mensaje=Error al cargar las licorerías";
        }
    }

    // Procesar selección de licorería
    @PostMapping("/seleccionar")
    public String seleccionarLicoreria(@RequestParam Long licoreriaId, 
                                     RedirectAttributes redirectAttrs, 
                                     Authentication auth,
                                     HttpServletRequest request) {
        try {
            logger.info("Iniciando selección de licorería ID: {}", licoreriaId);
            
            String username = auth.getName();
            logger.info("Usuario autenticado: {}", username);
            
            Usuario usuario = usuarioServicio.obtenerPorUsername(username);
            logger.info("Usuario encontrado: {}, Rol: {}", username, usuario.getRol());
            
            Licoreria licoreria = licoreriaServicio.obtenerPorId(licoreriaId)
                .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));
            logger.info("Licorería encontrada: {}", licoreria.getNombre());

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
            logger.info("Estableciendo licorería en el contexto: {}", licoreria.getNombre());
            licoreriaContext.setLicoreriaActual(licoreria);
            logger.info("Licorería establecida exitosamente en el contexto");
            
            return "redirect:/dashboard";
        } catch (Exception e) {
            logger.error("Error al seleccionar licorería: {}", e.getMessage(), e);
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
    @ResponseBody
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activar, RedirectAttributes redirectAttributes) {
        try {
            Licoreria licoreria = licoreriaServicio.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));
            
            // Si se está desactivando, verificar que no sea la licorería actual
            if (!activar && licoreriaContext.getLicoreriaActual() != null && 
                licoreriaContext.getLicoreriaActual().getId().equals(id)) {
                return "ERROR: No se puede desactivar la licorería actual";
            }
            
            licoreria.setEstado(activar);
            licoreriaServicio.guardar(licoreria);
            return "OK";
        } catch (Exception e) {
            logger.error("Error al cambiar estado de licorería: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    // Eliminar licorería
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public ResponseEntity<String> eliminarLicoreria(@PathVariable Long id) {
        try {
            logger.info("Iniciando proceso de eliminación de licorería ID: {}", id);
            
            Licoreria licoreria = licoreriaServicio.obtenerPorId(id)
                .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));
            
            // Verificar que no sea la licorería actual
            if (licoreriaContext.getLicoreriaActual() != null && 
                licoreriaContext.getLicoreriaActual().getId().equals(id)) {
                logger.warn("Intento de eliminar la licorería actual");
                return ResponseEntity.badRequest().body("No se puede eliminar la licorería actual");
            }
            
            // Eliminar todos los datos asociados
            logger.info("Eliminando licorería y sus datos asociados");
            licoreriaServicio.eliminarLicoreriaCompleta(id);
            
            logger.info("Licorería eliminada exitosamente");
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            logger.error("Error al eliminar licorería: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al eliminar la licorería: " + e.getMessage());
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