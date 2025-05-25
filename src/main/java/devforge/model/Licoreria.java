package devforge.model;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "licorerias")
public class Licoreria implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String telefono;

    @Column(name = "ip_local")
    private String ipLocal;

    @Column(name = "configuracion_impresora")
    private String configuracionImpresora;

    @Column(nullable = false)
    private boolean estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (estado == false) {
            estado = true; // Por defecto, las licorerías se crean activas
        }
    }

    // Método para validar que la licorería está activa
    public boolean isActiva() {
        return estado;
    }

    // Método para obtener un resumen de la licorería
    public String getResumen() {
        return String.format("%s - %s", nombre, direccion);
    }

    // Método para clonar la licorería (útil para la sesión)
    public Licoreria clone() {
        Licoreria clone = new Licoreria();
        clone.setId(this.id);
        clone.setNombre(this.nombre);
        clone.setDireccion(this.direccion);
        clone.setTelefono(this.telefono);
        clone.setIpLocal(this.ipLocal);
        clone.setConfiguracionImpresora(this.configuracionImpresora);
        clone.setEstado(this.estado);
        clone.setFechaCreacion(this.fechaCreacion);
        return clone;
    }
}