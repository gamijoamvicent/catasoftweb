package devforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "facturas")
@Data
public class Factura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, unique = true)
    private String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "tipo_factura", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoFactura tipoFactura;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_anulada_id")
    private Venta ventaAnulada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;

    @PrePersist
    protected void onCreate() {
        fechaEmision = LocalDateTime.now();
    }

    public enum TipoFactura {
        VENTA,
        ANULACION
    }
} 