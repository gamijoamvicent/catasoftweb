package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.UsuarioServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();

        // Lógica de selección automática de licorería
        if (licoreriaActual == null) {
            if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN) {
                // Para SUPER_ADMIN, redirigir a selección de licorería
                logger.info("SUPER_ADMIN sin licorería seleccionada, redirigiendo a selección");
                return "redirect:/licorerias/seleccionar";
            } else {
                // Para otros roles, seleccionar automáticamente su licorería asignada
                if (usuario.getLicoreria() != null) {
                    logger.info("Seleccionando automáticamente licorería para usuario {}: {}", 
                        username, usuario.getLicoreria().getNombre());
                    licoreriaContext.setLicoreriaActual(usuario.getLicoreria());
                    licoreriaActual = usuario.getLicoreria();
                } else {
                    logger.error("Usuario {} no tiene licorería asignada", username);
                    return "redirect:/error?mensaje=No tienes una licorería asignada";
                }
            }
        } else {
            // Verificar que el usuario tiene acceso a la licorería seleccionada
            if (usuario.getRol() != Usuario.Rol.SUPER_ADMIN && 
                !usuario.getLicoreriaId().equals(licoreriaActual.getId())) {
                logger.warn("Usuario {} intentando acceder a licorería no asignada", username);
                return "redirect:/acceso-denegado";
            }
        }

        logger.info("Mostrando dashboard para usuario: {} con licorería: {}", 
            username, licoreriaActual.getNombre());

        model.addAttribute("licoreriaActual", licoreriaActual);
        model.addAttribute("usuario", usuario);
        return "index";
    }
} 