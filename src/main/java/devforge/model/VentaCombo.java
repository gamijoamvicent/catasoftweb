package devforge.model;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "ventas_combo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class VentaCombo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;
    
    @Column(name = "valor_venta_usd", nullable = false)
    private BigDecimal valorVentaUSD;
    
    @Column(name = "valor_venta_bs", nullable = false)
    private BigDecimal valorVentaBS;
    
    @Column(name = "tasa_conversion", nullable = false)
    private BigDecimal tasaConversion;
    
    @ManyToOne
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;
    
    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;
      @Column(name = "fecha_venta", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaVenta;
}
