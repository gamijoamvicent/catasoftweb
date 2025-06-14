package devforge.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VentaComboDTO {
    private Long comboId;
    private BigDecimal valorVentaUSD;
    private BigDecimal valorVentaBS;
    private BigDecimal tasaConversion;
    private String metodoPago;
    private Integer cantidad;
}
