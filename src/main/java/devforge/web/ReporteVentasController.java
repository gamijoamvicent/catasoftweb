package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.UsuarioServicio;
import devforge.servicio.VentaServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Controller
@RequestMapping("/reportes/ventas")
public class ReporteVentasController {
    private static final Logger logger = LoggerFactory.getLogger(ReporteVentasController.class);

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private VentaServicio ventaServicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarReporteVentas(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            Model model,
            Authentication auth) {
        
        String username = auth.getName();
        Usuario usuario = usuarioServicio.obtenerPorUsername(username);
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();

        if (licoreriaActual == null) {
            logger.error("No hay licorería seleccionada para el usuario: {}", username);
            return "redirect:/error?mensaje=No hay licorería seleccionada";
        }

        // Si no se especifican fechas, usar el mes actual
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now().withDayOfMonth(1);
        }
        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }

        // Convertir LocalDate a LocalDateTime
        LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
        LocalDateTime fechaFinDateTime = fechaFin.atTime(LocalTime.MAX);

        // Obtener estadísticas de ventas
        Map<String, Double> ventasPorMetodoPago = ventaServicio.obtenerVentasPorMetodoPago(
            licoreriaActual.getId(), fechaInicioDateTime, fechaFinDateTime);
        
        double totalVentas = ventasPorMetodoPago.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        model.addAttribute("licoreriaActual", licoreriaActual);
        model.addAttribute("usuario", usuario);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("ventasPorMetodoPago", ventasPorMetodoPago);
        model.addAttribute("totalVentas", totalVentas);

        return "reportes/ventas";
    }
} 