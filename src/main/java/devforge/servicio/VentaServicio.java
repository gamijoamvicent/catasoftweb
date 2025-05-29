package devforge.servicio;

import devforge.model.Venta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VentaServicio {
    Venta guardar(Venta venta);
    List<Venta> listarVentasPorLicoreria(Long licoreriaId);
    List<Venta> listarVentasPorLicoreriaYFecha(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
    Map<String, Double> obtenerVentasPorMetodoPago(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
    Optional<Venta> buscarPorId(Long id);

    Page<Venta> listarVentasPaginadas(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago,
            Pageable pageable);

    byte[] generarReportePdf(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago) throws IOException;

    byte[] generarReporteExcel(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago) throws IOException;
} 