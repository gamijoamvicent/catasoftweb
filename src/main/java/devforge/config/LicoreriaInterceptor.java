package devforge.config;

import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import devforge.servicio.UsuarioServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LicoreriaInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LicoreriaInterceptor.class);

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private LicoreriaServicio licoreriaServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // Permitir acceso a rutas públicas
        if (isPublicPath(requestURI)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }

        String username = auth.getName();
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        
        if (usuario == null) {
            logger.error("Usuario no encontrado: {}", username);
            response.sendRedirect("/login");
            return false;
        }

        // Para SUPER_ADMIN
        if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN) {
            // Permitir acceso a la selección de licorería
            if (requestURI.equals("/licorerias/seleccionar")) {
                return true;
            }
            
            // Para otras rutas, verificar si hay licorería seleccionada
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                logger.info("SUPER_ADMIN sin licorería seleccionada, redirigiendo a selección");
                response.sendRedirect("/licorerias/seleccionar");
                return false;
            }
            return true;
        }

        // Para otros roles (ADMIN_LOCAL, CAJERO, BODEGA)
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        
        // Si no hay licorería seleccionada, seleccionar automáticamente la asignada
        if (licoreriaActual == null) {
            if (usuario.getLicoreriaId() != null) {
                Licoreria licoreriaAsignada = licoreriaServicio.obtenerPorId(usuario.getLicoreriaId())
                    .orElseThrow(() -> new RuntimeException("Licorería no encontrada"));
                logger.info("Seleccionando automáticamente licorería para usuario {}: {}", 
                    username, licoreriaAsignada.getNombre());
                licoreriaContext.setLicoreriaActual(licoreriaAsignada);
                return true;
            } else {
                logger.error("Usuario {} no tiene licorería asignada", username);
                response.sendRedirect("/error?mensaje=No tienes una licorería asignada");
                return false;
            }
        }

        // Verificar que el usuario pertenece a la licorería seleccionada
        if (!usuario.getLicoreriaId().equals(licoreriaActual.getId())) {
            logger.warn("Usuario {} intentando acceder a licorería no asignada", username);
            response.sendRedirect("/acceso-denegado");
            return false;
        }

        return true;
    }

    private boolean isPublicPath(String requestURI) {
        return requestURI.startsWith("/login") || 
               requestURI.startsWith("/css/") || 
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/error") ||
               requestURI.equals("/");
    }
} 