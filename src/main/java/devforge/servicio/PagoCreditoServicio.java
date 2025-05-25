package devforge.servicio;

import devforge.model.PagoCredito;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoCreditoServicio {
    PagoCredito registrarPago(Long creditoId, BigDecimal monto, 
                             PagoCredito.MetodoPago metodoPago,
                             String referencia, String observaciones);
    
    List<PagoCredito> listarPagosPorCredito(Long creditoId);
    
    List<PagoCredito> listarPagosPorLicoreria(Long licoreriaId);
    
    List<PagoCredito> listarPagosPorFecha(Long licoreriaId, 
                                         LocalDateTime fechaInicio,
                                         LocalDateTime fechaFin);
} 