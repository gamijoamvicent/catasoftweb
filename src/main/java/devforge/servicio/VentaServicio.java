package devforge.servicio;

import devforge.model.Venta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VentaServicio {
    Venta guardar(Venta venta);
    List<Venta> listarVentasPorLicoreria(Long licoreriaId);
    List<Venta> listarVentasPorLicoreriaYFecha(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
    Map<String, Double> obtenerVentasPorMetodoPago(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
} 