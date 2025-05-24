package devforge.config;

import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LicoreriaInterceptor implements HandlerInterceptor {

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private LicoreriaServicio licoreriaServicio;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // No interceptar rutas públicas
        if (request.getRequestURI().startsWith("/login") || 
            request.getRequestURI().startsWith("/css/") || 
            request.getRequestURI().startsWith("/js/") ||
            request.getRequestURI().startsWith("/error")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Usuario usuario = (Usuario) auth.getPrincipal();
            
            // Si el usuario es SUPER_ADMIN, permitir acceso sin licorería específica
            if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN) {
                return true;
            }

            // Si no hay licorería seleccionada, redirigir a la selección
            if (licoreriaContext.getLicoreriaActual() == null) {
                response.sendRedirect("/seleccionar-licoreria");
                return false;
            }

            // Verificar que el usuario pertenece a la licorería seleccionada
            if (!usuario.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                response.sendRedirect("/acceso-denegado");
                return false;
            }
        }

        return true;
    }
} 