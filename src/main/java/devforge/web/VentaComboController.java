package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.dto.VentaComboDTO;
import devforge.model.VentaCombo;
import devforge.model.Combo;
import devforge.servicio.VentaComboService;
import devforge.servicio.ComboService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/ventas/combos")
public class VentaComboController {

    private final VentaComboService ventaComboService;
    private final ComboService comboService;
    private final LicoreriaContext licoreriaContext;

    public VentaComboController(VentaComboService ventaComboService, 
                              ComboService comboService, 
                              LicoreriaContext licoreriaContext) {
        this.ventaComboService = ventaComboService;
        this.comboService = comboService;
        this.licoreriaContext = licoreriaContext;
    }    @PostMapping("/registrar")
    @ResponseBody
    public ResponseEntity<?> registrarVenta(@RequestBody VentaComboDTO ventaDTO) {
        try {
            // Validar licorería
            var licoreria = licoreriaContext.getLicoreriaActual();
            if (licoreria == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            // Validar campos requeridos
            if (ventaDTO.getComboId() == null || 
                ventaDTO.getValorVentaUSD() == null || 
                ventaDTO.getValorVentaBS() == null ||
                ventaDTO.getTasaConversion() == null ||
                ventaDTO.getMetodoPago() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Todos los campos son requeridos"));
            }

            // Buscar el combo
            Combo combo = comboService.obtenerPorId(ventaDTO.getComboId())
                .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado"));

            // Crear la venta
            VentaCombo venta = new VentaCombo();
            venta.setCombo(combo);
            venta.setValorVentaUSD(ventaDTO.getValorVentaUSD());
            venta.setValorVentaBS(ventaDTO.getValorVentaBS());
            venta.setTasaConversion(ventaDTO.getTasaConversion());
            venta.setMetodoPago(ventaDTO.getMetodoPago());
            venta.setLicoreria(licoreria);

            // Guardar la venta
            venta = ventaComboService.guardar(venta);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Venta registrada exitosamente",
                "ventaId", venta.getId()
            ));        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log the full error but return a generic message
            e.printStackTrace(); // You should use a proper logger in production
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error interno al procesar la venta"));
        }
    }

    @GetMapping("/licoreria/{licoreriaId}")
    @ResponseBody
    public ResponseEntity<?> obtenerVentasPorLicoreria(@PathVariable Long licoreriaId) {
        try {
            // Validar que el ID de la licorería no sea nulo
            if (licoreriaId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El ID de la licorería es requerido"));
            }

            // Validar permisos
            if (!licoreriaId.equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "No tienes permiso para ver las ventas de esta licorería"));
            }

            var ventas = ventaComboService.listarPorLicoreria(licoreriaId);
            return ResponseEntity.ok(ventas);

        } catch (Exception e) {
            // Log the full error but return a generic message
            e.printStackTrace(); // You should use a proper logger in production
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al obtener las ventas"));
        }
    }
}
