package devforge.config;

import devforge.model.Licoreria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.io.Serializable;

@Component
@SessionScope
public class LicoreriaContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(LicoreriaContext.class);
    
    private Long licoreriaId;
    private String nombre;
    private String direccion;
    private String telefono;
    private boolean estado;

    public Licoreria getLicoreriaActual() {
        try {
            if (licoreriaId == null) {
                logger.debug("No hay licorería seleccionada");
                return null;
            }
            
            logger.debug("Reconstruyendo licorería con ID: {}", licoreriaId);
            Licoreria licoreria = new Licoreria();
            licoreria.setId(licoreriaId);
            licoreria.setNombre(nombre);
            licoreria.setDireccion(direccion);
            licoreria.setTelefono(telefono);
            licoreria.setEstado(estado);
            return licoreria;
        } catch (Exception e) {
            logger.error("Error al reconstruir licorería: {}", e.getMessage(), e);
            return null;
        }
    }

    public void setLicoreriaActual(Licoreria licoreria) {
        try {
            if (licoreria != null) {
                logger.debug("Estableciendo licorería: {}", licoreria.getNombre());
                this.licoreriaId = licoreria.getId();
                this.nombre = licoreria.getNombre();
                this.direccion = licoreria.getDireccion();
                this.telefono = licoreria.getTelefono();
                this.estado = licoreria.isEstado();
            } else {
                logger.debug("Limpiando licorería del contexto");
                this.licoreriaId = null;
                this.nombre = null;
                this.direccion = null;
                this.telefono = null;
                this.estado = false;
            }
        } catch (Exception e) {
            logger.error("Error al establecer licorería: {}", e.getMessage(), e);
            throw new RuntimeException("Error al establecer la licorería en el contexto", e);
        }
    }

    public Long getLicoreriaId() {
        return licoreriaId;
    }

    public boolean hasLicoreriaSeleccionada() {
        return licoreriaId != null && estado;
    }

    public void clear() {
        logger.debug("Limpiando contexto de licorería");
        this.licoreriaId = null;
        this.nombre = null;
        this.direccion = null;
        this.telefono = null;
        this.estado = false;
    }

    @Override
    public String toString() {
        return "LicoreriaContext{" +
                "licoreriaId=" + licoreriaId +
                ", nombre='" + nombre + '\'' +
                ", estado=" + estado +
                '}';
    }
}