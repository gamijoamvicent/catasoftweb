package devforge.servicio;

import devforge.model.Combo;
import java.util.List;
import java.util.Map;

public interface VentaComboServicio {
    void registrarVenta(List<Map<String, Object>> items);
    void descontarStockCombo(Combo combo, int cantidad);
    void generarTicket(Long ventaId);
} 