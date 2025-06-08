package devforge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "cajas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio", nullable = false)
    private Double precio;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "cantidad_unidades")
    private Integer cantidadUnidades;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "producto_id", insertable = false, updatable = false)
    private Long productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;

    @ManyToOne
    @JoinColumn(name = "precio_dolar_id")
    private PrecioDolar precioDolar;

    @Column(name = "tipo_tasa")
    private String tipoTasa; // BCV, PROMEDIO, PARALELA

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "estado")
    private Boolean estado = true;

    // Campo calculado, no se almacena en la base de datos
    @Transient
    private Double tasaCambio;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public TipoTasa getTipoTasaEnum() {
        return tipoTasa != null ? TipoTasa.valueOf(tipoTasa) : null;
    }

    public void setTipoTasaEnum(TipoTasa tipoTasa) {
        this.tipoTasa = tipoTasa != null ? tipoTasa.name() : null;
    }

    // Método para obtener la tasa de cambio actual
    public Double getTasaCambio() {
        return precioDolar != null ? precioDolar.getPrecioDolar() : null;
    }
}