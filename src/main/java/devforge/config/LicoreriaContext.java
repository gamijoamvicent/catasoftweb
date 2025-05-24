package devforge.config;

import devforge.model.Licoreria;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class LicoreriaContext {
    private Licoreria licoreriaActual;

    public Licoreria getLicoreriaActual() {
        return licoreriaActual;
    }

    public void setLicoreriaActual(Licoreria licoreria) {
        this.licoreriaActual = licoreria;
    }

    public Long getLicoreriaId() {
        return licoreriaActual != null ? licoreriaActual.getId() : null;
    }

    public void clear() {
        this.licoreriaActual = null;
    }
} 