package devforge.web.api;

import devforge.dto.ApiResponse;
import devforge.servicio.VentaCajaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas-cajas")
public class VentaCajaApiController {

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
