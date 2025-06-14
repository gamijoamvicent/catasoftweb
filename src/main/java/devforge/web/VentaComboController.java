package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.dto.VentaComboDTO;
import devforge.model.VentaCombo;
import devforge.model.Combo;
import devforge.servicio.VentaComboService;
import devforge.servicio.ComboService;
import devforge.model.PrecioDolar;
import devforge.model.TipoTasa;
import devforge.repository.PrecioDolarRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/ventas/combos")
public class VentaComboController {
    private final VentaComboService ventaComboService;
    private final ComboService comboService;
    private final LicoreriaContext licoreriaContext;
    private final PrecioDolarRepository precioDolarRepository;
    
    public VentaComboController(
            VentaComboService ventaComboService, 
            ComboService comboService, 
            LicoreriaContext licoreriaContext,
            PrecioDolarRepository precioDolarRepository) {
        this.ventaComboService = ventaComboService;
        this.comboService = comboService;
        this.licoreriaContext = licoreriaContext;
        this.precioDolarRepository = precioDolarRepository;
    }
    
    @PostMapping("/registrar")
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
                
            // Validar que el combo pertenezca a la licorería actual
            if (!combo.getLicoreria().getId().equals(licoreria.getId())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El combo no pertenece a la licorería actual"));
            }            // Validar que el valor USD coincida con el precio del combo
            if (ventaDTO.getValorVentaUSD().compareTo(combo.getPrecio()) != 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El valor en USD no coincide con el precio del combo"));
            }            // Obtener y validar la tasa de cambio del combo
            String tipoTasaStr = combo.getTipoTasa().name();
            // Normalizar el tipo de tasa para asegurar que usamos PARALELA en lugar de PARALELO
            if ("PARALELO".equals(tipoTasaStr)) {
                tipoTasaStr = "PARALELA";
            }
            TipoTasa tipoTasa = TipoTasa.valueOf(tipoTasaStr);            // Buscar la tasa más reciente para la licorería actual
            List<PrecioDolar> tasasCombo = precioDolarRepository.findByLicoreriaIdAndTipoTasaOrderByFechaDolarDesc(
                licoreria.getId(), 
                PrecioDolar.TipoTasa.valueOf(tipoTasaStr)
            );
            if (tasasCombo.isEmpty()) {
                throw new IllegalStateException(
                    "No hay tasa disponible para el tipo " + tipoTasa + 
                    " en la licorería actual. Por favor actualice las tasas de cambio"
                );
            }
            PrecioDolar tasaCombo = tasasCombo.get(0);

            // Calcular el valor esperado en BS usando la tasa del combo
            BigDecimal tasaBs = BigDecimal.valueOf(tasaCombo.getPrecioDolar());
            BigDecimal valorBsEsperado = ventaDTO.getValorVentaUSD().multiply(tasaBs);

            // Verificar que el valor en BS enviado coincida con el calculado (con un margen de error de 0.01)
            if (ventaDTO.getValorVentaBS().subtract(valorBsEsperado).abs().compareTo(new BigDecimal("0.01")) > 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "El valor en Bs no coincide con la tasa de cambio del combo",
                        "valorEsperado", valorBsEsperado,
                        "tasaUsada", tasaBs
                    ));
            }
            // Validar cantidad
            if (ventaDTO.getCantidad() == null || ventaDTO.getCantidad() < 1) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe indicar la cantidad de combos a vender"));
            }

            // Crear la venta
            VentaCombo venta = new VentaCombo();
            venta.setCombo(combo);
            venta.setValorVentaUSD(ventaDTO.getValorVentaUSD());
            venta.setValorVentaBS(valorBsEsperado); // Usar el valor calculado
            venta.setTasaConversion(tasaBs);
            venta.setMetodoPago(ventaDTO.getMetodoPago());
            venta.setLicoreria(licoreria);
            venta.setCantidad(ventaDTO.getCantidad());
            
            // Decrementar stock de productos en el combo según la cantidad
            ventaComboService.decrementarStockProductos(combo, ventaDTO.getCantidad());
            
            // Guardar la venta
            venta = ventaComboService.guardar(venta);
            
            return ResponseEntity.ok(Map.of(
                "mensaje", "Venta registrada exitosamente",
                "ventaId", venta.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // Error específico para tasas de cambio no disponibles
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Log the full error but return a generic message
            e.printStackTrace(); // You should use a proper logger in production
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "Error interno al procesar la venta",
                    "detalle", e.getMessage()
                ));
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

    @PostMapping("/registrar-multiple")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> registrarVentaMultiple(@RequestBody List<VentaComboDTO> ventasDTO) {
        try {
            if (ventasDTO == null || ventasDTO.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El carrito está vacío"));
            }
            var licoreria = licoreriaContext.getLicoreriaActual();
            if (licoreria == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe seleccionar una licorería primero"));
            }
            // Validar y procesar todas las ventas en una sola transacción
            ventaComboService.procesarCarrito(ventasDTO, licoreria);
            return ResponseEntity.ok(Map.of("mensaje", "Venta(s) registrada(s) exitosamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
