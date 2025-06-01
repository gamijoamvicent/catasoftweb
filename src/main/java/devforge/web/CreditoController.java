package devforge.web;

import devforge.model.Cliente;
import devforge.model.Credito;
import devforge.servicio.ClienteServicio;
import devforge.servicio.CreditoServicio;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/creditos")
public class CreditoController {

    private final CreditoServicio creditoServicio;
    private final ClienteServicio clienteServicio;
    private final LicoreriaContext licoreriaContext;

    public CreditoController(
            CreditoServicio creditoServicio,
            ClienteServicio clienteServicio,
            LicoreriaContext licoreriaContext) {
        this.creditoServicio = creditoServicio;
        this.clienteServicio = clienteServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping
    public String listarCreditos(
            @RequestParam(required = false) String estado,
            Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        List<Credito> creditos;
        if (estado != null && !estado.isEmpty()) {
            switch (estado) {
                case "PAGADO_TOTAL":
                    creditos = creditoServicio.listarCreditosPorLicoreriaYEstado(
                        licoreriaContext.getLicoreriaId(), "PAGADO_TOTAL");
                    break;
                case "PAGADO_PARCIAL":
                    creditos = creditoServicio.listarCreditosPorLicoreriaYEstado(
                        licoreriaContext.getLicoreriaId(), "PAGADO_PARCIAL");
                    break;
                case "PENDIENTE":
                    creditos = creditoServicio.listarCreditosPorLicoreriaYEstado(
                        licoreriaContext.getLicoreriaId(), "PENDIENTE");
                    break;
                case "VENCIDO":
                    creditos = creditoServicio.listarCreditosVencidos(
                        licoreriaContext.getLicoreriaId());
                    break;
                default:
                    creditos = creditoServicio.listarCreditosPorLicoreria(
                        licoreriaContext.getLicoreriaId());
            }
        } else {
            creditos = creditoServicio.listarCreditosPorLicoreria(
                licoreriaContext.getLicoreriaId());
        }

        model.addAttribute("creditos", creditos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        model.addAttribute("estadoSeleccionado", estado);

        return "creditos/listar";
    }

    @GetMapping("/cliente/{clienteId}")
    public String verCreditosCliente(@PathVariable Long clienteId, Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        Cliente cliente = clienteServicio.buscarPorId(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!cliente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
            throw new RuntimeException("No tienes permiso para ver los créditos de este cliente");
        }

        List<Credito> creditos = creditoServicio.listarCreditosPorCliente(
            clienteId, licoreriaContext.getLicoreriaId());

        model.addAttribute("cliente", cliente);
        model.addAttribute("creditos", creditos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());

        return "creditos/cliente";
    }

    @PostMapping("/{creditoId}/pagar")
    @ResponseBody
    public ResponseEntity<?> registrarPago(
            @PathVariable Long creditoId,
            @RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            Double montoPago = Double.valueOf(payload.get("montoPago").toString());
            if (montoPago <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El monto debe ser mayor a cero"));
            }

            Credito credito = creditoServicio.buscarPorId(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));

            if (!credito.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No tienes permiso para registrar pagos en este crédito"));
            }

            creditoServicio.registrarPago(creditoId, BigDecimal.valueOf(montoPago));

            return ResponseEntity.ok(Map.of(
                "mensaje", "✅ Pago registrado exitosamente",
                "creditoId", creditoId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "❌ Error al registrar el pago: " + e.getMessage()));
        }
    }

    @GetMapping("/pendientes")
    public String listarCreditosPendientes(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        List<Credito> creditosPendientes = creditoServicio.listarCreditosPendientes(
            licoreriaContext.getLicoreriaId());

        model.addAttribute("creditos", creditosPendientes);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());

        return "creditos/pendientes";
    }

    @GetMapping("/vencidos")
    public String listarCreditosVencidos(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        List<Credito> creditosVencidos = creditoServicio.listarCreditosVencidos(
            licoreriaContext.getLicoreriaId());

        model.addAttribute("creditos", creditosVencidos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());

        return "creditos/vencidos";
    }
} 