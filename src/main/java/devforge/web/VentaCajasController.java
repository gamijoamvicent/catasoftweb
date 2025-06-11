package devforge.web;

import devforge.model.Caja;
import devforge.model.Licoreria;
import devforge.model.PrecioDolar;
import devforge.model.Venta;
import devforge.model.VentaCaja;
import devforge.servicio.CajaServicio;
import devforge.servicio.PrecioDolarServicio;
import devforge.servicio.VentaCajaServicio;
import devforge.config.LicoreriaContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.csrf.CsrfToken;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Controller
@RequestMapping("/ventas/cajas")
public class VentaCajasController {

    @Autowired
    private VentaCajaServicio ventaCajaServicio;
    
    @Autowired
    private CajaServicio cajaServicio;
    
    @Autowired
    private PrecioDolarServicio precioDolarServicio;
    
    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping("/nueva")
    public String mostrarVentaCajas(Model model, HttpServletRequest request) {
        // Verificar si hay una licorería seleccionada
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        
        // Obtener la licorería actual
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        
        // Obtener todas las cajas disponibles
        List<Caja> cajas = cajaServicio.listarCajasPorLicoreria(licoreriaActual.getId());
        
        // Agregar datos al modelo
        model.addAttribute("cajas", cajas);
        model.addAttribute("licoreria", licoreriaActual);
        
        // Agregar el token CSRF al modelo
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            model.addAttribute("_csrf", token);
        }
        
        return "ventas/venta-cajas";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<?> buscarCajas(@RequestParam String termino) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            List<Caja> cajas = cajaServicio.buscarPorNombre(termino, licoreriaActual.getId());
            return ResponseEntity.ok(cajas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al buscar cajas: " + e.getMessage());
        }
    }

    @PostMapping("/procesar")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<?> procesarVenta(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "❌ Debe seleccionar una licorería primero"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            if (items == null || items.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "❌ No hay items para procesar"));
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

                    Caja caja = cajaServicio.buscarPorId(cajaId)
                        .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

                    // Solo validar stock si la caja tiene un producto asociado
                    if (caja.getProducto() != null) {
                        Integer stockProducto = caja.getProducto().getCantidad();
                        if (stockProducto == null) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", 
                                    String.format("El producto %s no tiene stock definido",
                                        caja.getProducto().getNombre())));
                        }

                        int unidadesNecesarias = caja.getCantidadUnidades() * cantidad;
                        if (stockProducto < unidadesNecesarias) {
                            return ResponseEntity.badRequest()
                                .body(Map.of("error", 
                                    String.format("Stock insuficiente para el producto: %s. Stock disponible: %d",
                                        caja.getProducto().getNombre(), stockProducto)));
                        }
                    }

                    // Validar tipo de tasa
                    try {
                        PrecioDolar.TipoTasa.valueOf(tipoTasa);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", "Tipo de tasa inválido: " + tipoTasa));
                    }
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Formato de número inválido en los datos: " + e.getMessage()));
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Error al procesar el item: " + e.getMessage()));
                }
            }

            // Procesar la venta
            try {
                ventaCajaServicio.registrarVenta(items);

                return ResponseEntity.ok()
                    .body(Map.of(
                        "success", true,
                        "message", "✅ Venta procesada exitosamente",
                        "clearCart", true
                    ));
            } catch (RuntimeException e) {
                String mensaje = e.getMessage();
                if (mensaje.contains("inactivo")) {
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", false,
                            "message", "❌ " + mensaje
                        ));
                } else if (mensaje.contains("Stock insuficiente")) {
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", false,
                            "message", "❌ " + mensaje
                        ));
                } else {
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "success", false,
                            "message", "❌ Error al procesar la venta: " + mensaje
                        ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Para logging
            return ResponseEntity.status(500)
                .body(Map.of(
                    "success", false,
                    "message", "❌ Error interno del servidor. Por favor, intente nuevamente."
                ));
        }
    }

    @GetMapping("/obtener-tasa")
    @ResponseBody
    public ResponseEntity<Double> obtenerTasaCambioActual() {
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        if (licoreriaActual == null) {
            return ResponseEntity.badRequest().build();
        }

        Double tasaCambio = precioDolarServicio.obtenerTasaCambioActual(licoreriaActual.getId());
        return ResponseEntity.ok(tasaCambio);
    }

    @PostMapping("/desactivar/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> desactivarVentaCaja(@PathVariable("id") Long ventaCajaId) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            boolean resultado = ventaCajaServicio.desactivarVentaCaja(ventaCajaId);

            if (resultado) {
                return ResponseEntity.ok()
                    .body(Map.of(
                        "success", true,
                        "message", "Venta desactivada exitosamente"
                    ));
            } else {
                return ResponseEntity.status(404)
                    .body(Map.of(
                        "success", false,
                        "error", "No se encontró la venta especificada"
                    ));
            }
        } catch (Exception e) {
            e.printStackTrace(); // Para logging
            return ResponseEntity.status(500)
                .body(Map.of(
                    "success", false,
                    "error", "Error al desactivar la venta: " + e.getMessage()
                ));
        }
    }
}
