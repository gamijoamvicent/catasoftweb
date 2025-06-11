package devforge.web.api;

import devforge.config.LicoreriaContext;
import devforge.dto.ApiResponse;
import devforge.servicio.VentaCajaServicio;
import devforge.web.dto.VentaCajaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas-cajas")
public class VentaCajaApiController {

    private final LicoreriaContext licoreriaContext;

    @Autowired
    public VentaCajaApiController(VentaCajaServicio ventaCajaServicio, LicoreriaContext licoreriaContext) {
        this.ventaCajaServicio = ventaCajaServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/reporte")
    public ResponseEntity<Map<String, Object>> obtenerReporteVentasCajas(
            @RequestParam(name = "fechaInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(name = "tipoCaja", required = false) String tipoCaja) {

        // Establecer fechas por defecto si no se proporcionan
        if (fechaInicio == null) fechaInicio = LocalDate.now();
        if (fechaFin == null) fechaFin = LocalDate.now();

        // Convertir fechas a LocalDateTime
        LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
        LocalDateTime fechaFinDateTime = fechaFin.atTime(LocalTime.MAX);

        // Obtener ID de la licorería actual
        Long licoreriaId = licoreriaContext.getLicoreriaActual().getId();

        // Obtener ventas de cajas según filtros
        List<VentaCajaDTO> ventas = ventaCajaServicio.buscarVentasCajasPorFechaYTipo(
                licoreriaId,
                fechaInicioDateTime,
                fechaFinDateTime,
                tipoCaja
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", ventas);
        response.put("verDetalle", false); // Añadir flag para ocultar el botón de detalle
        return ResponseEntity.ok(response);
    }

    @Autowired
    private VentaCajaServicio ventaCajaServicio;

    /**
     * Endpoint para inactivar una venta de caja (cambiar su estado activo a false)
     * @param id ID de la venta de caja a inactivar
     * @return Respuesta con el resultado de la operación
     */
    @PostMapping("/{id}/inactivar")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse> inactivarVentaCaja(@PathVariable Long id) {
        try {
            boolean resultado = ventaCajaServicio.inactivarVentaCaja(id);
            if (resultado) {
                return ResponseEntity.ok(new ApiResponse(true, "Venta de caja inactivada correctamente"));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "No se pudo inactivar la venta de caja"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error al inactivar venta de caja: " + e.getMessage()));
        }
    }
}
