package devforge.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VentaComboDashboardDTO {
    public Long id;
    public String comboNombre;
    public BigDecimal valorVentaUSD;
    public BigDecimal valorVentaBS;
    public BigDecimal tasaConversion;
    public String metodoPago;
    public LocalDateTime fechaVenta;
    public String clienteNombre;
}
