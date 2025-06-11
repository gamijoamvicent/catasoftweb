package devforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "venta_caja")
@Data
public class VentaCaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = true)
    private Venta venta;

    @Column(name = "venta_id", insertable = false, updatable = false)
    private Long ventaId;

    @Column(name = "licoreria_id")
    private Long licoreriaId;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @Column(name = "caja_id", insertable = false, updatable = false)
    private Long cajaId;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tasa_cambio_usado", precision = 10, scale = 2)
    private BigDecimal tasaCambioUsado;

    @Column(name = "subtotal_bolivares", precision = 10, scale = 2)
    private BigDecimal subtotalBolivares;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tasa_usado")
    private PrecioDolar.TipoTasa tipoTasa;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        // El campo activo ya tiene valor predeterminado true
    }
}
