package devforge.servicio;

import devforge.model.VentaCaja;

import java.util.List;
import java.util.Map;

public interface VentaCajaServicio {
    void registrarVenta(List<Map<String, Object>> items);

    void descontarStockCaja(Long cajaId, int cantidad);

    List<VentaCaja> listarVentasPorVenta(Long ventaId);

    List<VentaCaja> listarVentasPorCaja(Long cajaId);
}
