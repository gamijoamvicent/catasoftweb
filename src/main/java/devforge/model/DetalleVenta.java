package devforge.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_venta")
@Data
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(name = "venta_id", insertable = false, updatable = false)
    private Long ventaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "producto_id", insertable = false, updatable = false)
    private Long productoId;

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
    private PrecioDolar.TipoTasa tipoTasaUsado;
}