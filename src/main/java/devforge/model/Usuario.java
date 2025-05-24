package devforge.model;

import jakarta.persistence.*;
import jakarta.persistence.FetchType;
import lombok.*;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    private Rol rol; // ADMIN, CAJERO, BODEGA

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;

    public enum Rol {
        SUPER_ADMIN,    // Acceso total a todas las licorerías
        ADMIN_LOCAL,    // Administrador de una licorería específica
        CAJERO,         // Cajero de una licorería específica
        BODEGA          // Encargado de bodega de una licorería específica
    }
}