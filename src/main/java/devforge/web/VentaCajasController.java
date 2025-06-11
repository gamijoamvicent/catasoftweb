package devforge.web;

import devforge.model.Caja;
import devforge.model.Licoreria;
import devforge.model.PrecioDolar;
import devforge.model.Venta;
import devforge.model.VentaCaja;
import devforge.model.enums.TipoVenta;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public ResponseEntity<?> procesarVenta(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "❌ Debe seleccionar una licorería primero"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            // Validar el tipo de venta
            String tipoVenta = (String) payload.get("tipoVenta");
            if (tipoVenta == null) {
                tipoVenta = "CONTADO"; // Valor por defecto
            }

            // Validar TipoVenta enum
            try {
                TipoVenta.valueOf(tipoVenta);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "❌ Tipo de venta inválido: " + tipoVenta));
            }

            // Procesar ID de cliente
            Long clienteId = null;
            if (payload.containsKey("clienteId") && payload.get("clienteId") != null 
                && !payload.get("clienteId").toString().trim().isEmpty()) {
                try {
                    clienteId = Long.valueOf(payload.get("clienteId").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "❌ ID de cliente inválido"));
                }
            }

            // Validar cliente para ventas a crédito
            if (TipoVenta.valueOf(tipoVenta) == TipoVenta.CREDITO && clienteId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "❌ Para ventas a crédito debe seleccionar un cliente"));
            }

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
                ventaCajaServicio.registrarVenta(items, tipoVenta, clienteId);

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

    @GetMapping("/detalle-ajax/{id}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetalleVentaCaja(@PathVariable("id") Long ventaCajaId) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            // Obtener la venta de caja
            Optional<VentaCaja> ventaCajaOpt = ventaCajaServicio.buscarPorId(ventaCajaId);
            if (!ventaCajaOpt.isPresent()) {
                return ResponseEntity.status(404)
                    .body(Map.of("error", "Venta de caja no encontrada"));
            }

            VentaCaja ventaCaja = ventaCajaOpt.get();

            // Crear un mapa con la información relevante
            Map<String, Object> detalleVenta = new HashMap<>();
            detalleVenta.put("id", ventaCaja.getId());
            detalleVenta.put("fechaCreacion", ventaCaja.getFechaCreacion());
            detalleVenta.put("cajaNombre", ventaCaja.getCaja().getNombre());
            detalleVenta.put("cantidad", ventaCaja.getCantidad());
            detalleVenta.put("precioUnitario", ventaCaja.getPrecioUnitario());
            detalleVenta.put("subtotal", ventaCaja.getSubtotal());
            detalleVenta.put("tipoTasa", ventaCaja.getTipoTasa());
            detalleVenta.put("tasaCambioUsado", ventaCaja.getTasaCambioUsado());
            detalleVenta.put("subtotalBolivares", ventaCaja.getSubtotalBolivares());

            // Agregar información de la venta si existe
            if (ventaCaja.getVenta() != null) {
                Map<String, Object> ventaInfo = new HashMap<>();
                ventaInfo.put("id", ventaCaja.getVenta().getId());
                ventaInfo.put("fechaVenta", ventaCaja.getVenta().getFechaVenta());
                ventaInfo.put("metodoPago", ventaCaja.getVenta().getMetodoPago());
                ventaInfo.put("tipoVenta", ventaCaja.getVenta().getTipoVenta());
                ventaInfo.put("totalVenta", ventaCaja.getVenta().getTotalVenta());
                ventaInfo.put("totalVentaBs", ventaCaja.getVenta().getTotalVentaBs());

                // Información del cliente si existe
                if (ventaCaja.getVenta().getCliente() != null) {
                    Map<String, Object> clienteInfo = new HashMap<>();
                    clienteInfo.put("id", ventaCaja.getVenta().getCliente().getId());
                    clienteInfo.put("nombre", ventaCaja.getVenta().getCliente().getNombre());
                    clienteInfo.put("documento", ventaCaja.getVenta().getCliente().getCedula());
                    ventaInfo.put("cliente", clienteInfo);
                }

                detalleVenta.put("venta", ventaInfo);
            }

            // Información de la caja
            Map<String, Object> cajaInfo = new HashMap<>();
            if (ventaCaja.getCaja() != null) {
                cajaInfo.put("id", ventaCaja.getCaja().getId());
                cajaInfo.put("nombre", ventaCaja.getCaja().getNombre());
                cajaInfo.put("tipo", ventaCaja.getCaja().getTipo());
                cajaInfo.put("cantidadUnidades", ventaCaja.getCaja().getCantidadUnidades());

                // Información del producto si existe
                if (ventaCaja.getCaja().getProducto() != null) {
                    Map<String, Object> productoInfo = new HashMap<>();
                    productoInfo.put("id", ventaCaja.getCaja().getProducto().getId());
                    productoInfo.put("nombre", ventaCaja.getCaja().getProducto().getNombre());
                    // No hay campo de código en la clase Producto
                    if (ventaCaja.getCaja().getProducto().getMarca() != null) {
                        productoInfo.put("codigo", ventaCaja.getCaja().getProducto().getMarca());
                    } else {
                        productoInfo.put("codigo", "");
                    }
                    cajaInfo.put("producto", productoInfo);
                }
            }

            detalleVenta.put("caja", cajaInfo);

            return ResponseEntity.ok(detalleVenta);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al obtener el detalle: " + e.getMessage()));
        }
    }
}
