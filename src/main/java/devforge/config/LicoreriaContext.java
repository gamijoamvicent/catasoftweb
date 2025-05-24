package devforge.config;

import devforge.model.Licoreria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LicoreriaContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(LicoreriaContext.class);
    private static final String LICORERIA_SESSION_KEY = "licoreriaActual";

    private Licoreria licoreriaActual;

    private HttpSession getSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getSession();
        }
        return null;
    }

    public Licoreria getLicoreriaActual() {
        if (licoreriaActual == null) {
            HttpSession session = getSession();
            if (session != null) {
                licoreriaActual = (Licoreria) session.getAttribute(LICORERIA_SESSION_KEY);
                logger.debug("Obteniendo licorería de sesión: {}", 
                    licoreriaActual != null ? licoreriaActual.getNombre() : "null");
            }
        }
        return licoreriaActual;
    }

    public void setLicoreriaActual(Licoreria licoreria) {
        HttpSession session = getSession();
        if (session != null) {
            if (licoreria != null) {
                logger.info("Estableciendo licorería actual: {} (ID: {})", 
                    licoreria.getNombre(), licoreria.getId());
                this.licoreriaActual = licoreria;
                session.setAttribute(LICORERIA_SESSION_KEY, licoreria);
            } else {
                logger.warn("Intentando establecer licorería actual como null");
                this.licoreriaActual = null;
                session.removeAttribute(LICORERIA_SESSION_KEY);
            }
        } else {
            logger.error("No se pudo obtener la sesión HTTP");
        }
    }

    public Long getLicoreriaId() {
        Long id = licoreriaActual != null ? licoreriaActual.getId() : null;
        logger.debug("Obteniendo ID de licorería actual: {}", id);
        return id;
    }

    public void clear() {
        logger.info("Limpiando contexto de licorería");
        HttpSession session = getSession();
        if (session != null) {
            this.licoreriaActual = null;
            session.removeAttribute(LICORERIA_SESSION_KEY);
        }
    }

    @Override
    public String toString() {
        return "LicoreriaContext{" +
               "licoreriaActual=" + (licoreriaActual != null ? 
                   licoreriaActual.getNombre() + " (ID: " + licoreriaActual.getId() + ")" : "null") +
               '}';
    }
} 