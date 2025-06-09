package devforge.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VentaCajaDTO {
    private Long id;
    private Long ventaId;
    private Long cajaId;
    private String cajaNombre;
    private String tipoCaja;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private BigDecimal tasaCambio;
    private BigDecimal subtotalBolivares;
    private String tipoTasa;
    private LocalDateTime fechaCreacion;

    // Campos adicionales para el dashboard
    private String nombreCliente;
    private String metodoPago;
}
