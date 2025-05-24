package devforge.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "licorerias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Licoreria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    private String telefono;

    @Column(name = "ip_local")
    private String ipLocal;

    @Column(name = "configuracion_impresora")
    private String configuracionImpresora;

    @Column(name = "estado")
    private boolean activo = true;

    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = new java.util.Date();
    }
}