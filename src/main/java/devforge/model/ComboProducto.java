package devforge.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "combo_productos")
public class ComboProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
} 