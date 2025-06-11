package devforge.servicio;

import devforge.model.VentaCaja;
import devforge.web.dto.VentaCajaDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface VentaCajaServicio {
    void registrarVenta(List<Map<String, Object>> items);

    void registrarVenta(List<Map<String, Object>> items, String tipoVenta, Long clienteId);

    void descontarStockCaja(Long cajaId, int cantidad);

    List<VentaCaja> listarVentasPorVenta(Long ventaId);

    List<VentaCaja> listarVentasPorCaja(Long cajaId);

    List<VentaCajaDTO> buscarVentasCajasPorFechaYTipo(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin, String tipoCaja);

    /**
     * Inactiva una venta de caja cambiando su estado 'activo' a false
     * @param id ID de la venta de caja a inactivar
     * @return true si la operación fue exitosa, false en caso contrario
     */
    boolean inactivarVentaCaja(Long id);

    /**
     * Desactiva una venta de caja (borrado lógico)
     * @param ventaCajaId ID de la venta de caja a desactivar
     * @return true si se desactivó correctamente, false si no se encontró
     */
    boolean desactivarVentaCaja(Long ventaCajaId);

    /**
     * Busca una venta de caja por su ID
     * @param id ID de la venta de caja a buscar
     * @return Optional con la venta de caja o vacío si no existe
     */
    Optional<VentaCaja> buscarPorId(Long id);
}
