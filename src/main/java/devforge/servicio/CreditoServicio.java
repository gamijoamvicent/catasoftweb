package devforge.servicio;

import devforge.model.Credito;
import devforge.model.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CreditoServicio {
    Credito crearCredito(Venta venta);
    Credito registrarPago(Long creditoId, BigDecimal montoPago);
    List<Credito> listarCreditosPorCliente(Long clienteId, Long licoreriaId);
    List<Credito> listarCreditosPendientes(Long licoreriaId);
    Optional<Credito> buscarPorId(Long id);
    void actualizarEstadoCredito(Long creditoId);
    List<Credito> listarCreditosVencidos(Long licoreriaId);
    List<Credito> listarCreditosPorLicoreria(Long licoreriaId);
    Map<String, Double> obtenerEstadisticasCreditos(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
} 