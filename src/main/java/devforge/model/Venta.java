package devforge.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
@Data
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "total_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVenta;

    @Column(name = "metodo_pago", nullable = false)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles;

    @PrePersist
    protected void onCreate() {
        fechaVenta = LocalDateTime.now();
    }

    public enum MetodoPago {
        EFECTIVO,
        TRANSFERENCIA,
        PUNTO_VENTA,
        PAGO_MOVIL
    }
} 