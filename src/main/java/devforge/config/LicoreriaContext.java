package devforge.config;

import devforge.model.Licoreria;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.io.Serializable;

@Component
@SessionScope
public class LicoreriaContext implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Licoreria licoreriaActual;

    public Licoreria getLicoreriaActual() {
        return licoreriaActual;
    }

    public void setLicoreriaActual(Licoreria licoreriaActual) {
        if (licoreriaActual != null) {
            // Usar el método clone para evitar problemas de serialización
            this.licoreriaActual = licoreriaActual.clone();
        } else {
            this.licoreriaActual = null;
        }
    }

    public Long getLicoreriaId() {
        return licoreriaActual != null ? licoreriaActual.getId() : null;
    }

    public boolean hasLicoreriaSeleccionada() {
        return licoreriaActual != null && licoreriaActual.isActiva();
    }
}