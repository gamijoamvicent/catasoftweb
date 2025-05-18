package devforge.model;

import jakarta.persistence.*;
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

    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    private Rol rol; // ADMIN, CAJERO, BODEGA

    public enum Rol {
        ADMIN,
        CAJERO,
        BODEGA
    }
}