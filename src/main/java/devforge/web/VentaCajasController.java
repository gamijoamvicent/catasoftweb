package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.PrecioDolar;
import devforge.model.Caja;
import devforge.model.Venta;
import devforge.model.VentaCaja;
import devforge.servicio.PrecioDolarServicio;
import devforge.servicio.CajaServicio;
import devforge.servicio.VentaCajaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/ventas/cajas")
public class VentaCajasController {

    private final LicoreriaContext licoreriaContext;
    private final PrecioDolarServicio precioDolarServicio;
    private final CajaServicio cajaServicio;
    private final VentaCajaServicio ventaCajaServicio;

    @Autowired
    public VentaCajasController(LicoreriaContext licoreriaContext, 
                               PrecioDolarServicio precioDolarServicio,
                               CajaServicio cajaServicio,
                               VentaCajaServicio ventaCajaServicio) {
        this.licoreriaContext = licoreriaContext;
        this.precioDolarServicio = precioDolarServicio;
        this.cajaServicio = cajaServicio;
        this.ventaCajaServicio = ventaCajaServicio;
    }

    @GetMapping("/nueva")
    public String mostrarVentaCajas(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "ventas/venta-cajas";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<?> buscarCajas(@RequestParam String termino) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            List<Caja> cajas = cajaServicio.buscarPorNombre(termino, licoreriaContext.getLicoreriaId());
            return ResponseEntity.ok(cajas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al buscar cajas: " + e.getMessage());
        }
    }

    @PostMapping("/procesar")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> procesarVenta(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay items para procesar"));
            }

            // Validar formato de datos
            for (Map<String, Object> item : items) {
                if (!item.containsKey("cajaId") || !item.containsKey("cantidad") || 
                    !item.containsKey("precio") || !item.containsKey("tipoTasa")) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Formato de datos inválido. Faltan campos requeridos"));
                }

                try {
                    Long cajaId = Long.valueOf(item.get("cajaId").toString());
                    int cantidad = Integer.valueOf(item.get("cantidad").toString());
                    double precio = Double.valueOf(item.get("precio").toString());
                    String tipoTasa = item.get("tipoTasa").toString();

                    if (cantidad <= 0) {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", "La cantidad debe ser mayor a 0"));
                    }

                    if (precio <= 0) {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", "El precio debe ser mayor a 0"));
                    }

                    // Validar tipo de tasa
                    try {
                        PrecioDolar.TipoTasa.valueOf(tipoTasa);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", "Tipo de tasa inválido: " + tipoTasa));
                    }

                    Caja caja = cajaServicio.buscarPorId(cajaId)
                        .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

                    if (caja.getStock() < cantidad) {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", 
                                String.format("Stock insuficiente para la caja: %s. Stock disponible: %d",
                                    caja.getNombre(), caja.getStock())));
                    }

                    // Si la caja tiene un producto asociado, validar su stock
                    if (caja.getProducto() != null) {
                        int unidadesNecesarias = caja.getCantidadUnidades() * cantidad;
                        if (caja.getProducto().getCantidad() < unidadesNecesarias) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", 
                                    String.format("Stock insuficiente para el producto: %s. Stock disponible: %d",
                                        caja.getProducto().getNombre(), caja.getProducto().getCantidad())));
                        }
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Formato de número inválido en los datos"));
                }
            }

            // Procesar la venta
            ventaCajaServicio.registrarVenta(items);

            return ResponseEntity.ok()
                .body(Map.of(
                    "success", true,
                    "message", "Venta procesada exitosamente"
                ));
        } catch (Exception e) {
            e.printStackTrace(); // Para logging
            return ResponseEntity.status(500)
                .body(Map.of(
                    "success", false,
                    "error", "Error al procesar la venta: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/obtener-tasa")
    @ResponseBody
    public ResponseEntity<Double> obtenerTasaCambioActual() {
        Long licoreriaId = licoreriaContext.getLicoreriaId();
        if (licoreriaId == null) {
            return ResponseEntity.badRequest().build();
        }

        Double tasaCambio = precioDolarServicio.obtenerTasaCambioActual(licoreriaId);
        return ResponseEntity.ok(tasaCambio);
    }
}
