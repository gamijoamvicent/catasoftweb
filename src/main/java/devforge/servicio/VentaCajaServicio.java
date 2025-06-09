package devforge.servicio;

import devforge.model.VentaCaja;
import devforge.web.dto.VentaCajaDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VentaCajaServicio {
    void registrarVenta(List<Map<String, Object>> items);

    void descontarStockCaja(Long cajaId, int cantidad);

    List<VentaCaja> listarVentasPorVenta(Long ventaId);

    List<VentaCaja> listarVentasPorCaja(Long cajaId);

    List<VentaCajaDTO> buscarVentasCajasPorFechaYTipo(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin, String tipoCaja);
}
