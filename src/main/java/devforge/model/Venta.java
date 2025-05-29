package devforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import devforge.model.enums.MetodoPago;
import devforge.model.enums.TipoVenta;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "total_venta_bs", precision = 10, scale = 2)
    private BigDecimal totalVentaBs;

    @Column(name = "metodo_pago", nullable = false)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Column(name = "tipo_venta", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoVenta tipoVenta = TipoVenta.CONTADO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Long clienteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "venta", cascade = CascadeType.ALL)
    private Credito credito;

    @PrePersist
    protected void onCreate() {
        fechaVenta = LocalDateTime.now();
    }

    public Double getTotalVenta() {
        return this.totalVenta != null ? this.totalVenta.doubleValue() : 0.0;
    }

    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta;
        // Calcular el total en bolívares usando la tasa de cambio actual
        if (totalVenta != null && !detalles.isEmpty() && detalles.get(0).getTasaCambioUsado() != null) {
            this.totalVentaBs = totalVenta.multiply(detalles.get(0).getTasaCambioUsado());
        }
    }

    public BigDecimal getTotalVentaBs() {
        if (this.totalVentaBs == null || this.totalVentaBs.compareTo(BigDecimal.ZERO) == 0) {
            if (this.totalVenta != null && this.detalles != null && !this.detalles.isEmpty()) {
                // Buscar el primer detalle con tasa válida
                for (DetalleVenta det : this.detalles) {
                    if (det.getTasaCambioUsado() != null && det.getTasaCambioUsado().compareTo(BigDecimal.ZERO) > 0) {
                        return this.totalVenta.multiply(det.getTasaCambioUsado());
                    }
                }
            }
            return BigDecimal.ZERO;
        }
        return this.totalVentaBs;
    }
}