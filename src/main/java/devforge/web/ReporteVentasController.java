package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.model.Venta;
import devforge.model.DetalleVenta;
import devforge.model.PrecioDolar;
import devforge.model.Producto;
import devforge.model.enums.TipoVenta;
import devforge.model.enums.MetodoPago;
import devforge.servicio.UsuarioServicio;
import devforge.servicio.VentaServicio;
import devforge.servicio.CreditoServicio;
import devforge.servicio.PrecioDolarServicio;
import devforge.servicio.ProductoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;
import java.math.BigDecimal;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.PdfPageEvent;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;

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

    @Autowired
    private CreditoServicio creditoServicio;

    @Autowired
    private PrecioDolarServicio precioDolarServicio;

    @Autowired
    private ProductoServicio productoServicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarDashboard(Model model) {
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        if (licoreriaActual == null) {
            return "redirect:/error?mensaje=No hay licorería seleccionada";
        }

        model.addAttribute("licoreriaActual", licoreriaActual);
        return "reportes/dashboard";
    }

    @GetMapping("/lista")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarListaVentas(Model model) {
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        if (licoreriaActual == null) {
            return "redirect:/error?mensaje=No hay licorería seleccionada";
        }

        model.addAttribute("licoreriaActual", licoreriaActual);
        return "reportes/lista";
    }

    @GetMapping("/data")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> obtenerDatosDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "TODAS") String tipoVenta,
            @RequestParam(required = false, defaultValue = "TODOS") String metodoPago) {
        
        try {
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay licorería seleccionada"));
            }

            // Si no se especifican fechas, usar el mes actual
            LocalDate now = LocalDate.now();
            fechaInicio = fechaInicio != null ? fechaInicio : now.withDayOfMonth(1);
            fechaFin = fechaFin != null ? fechaFin : now;

            // Convertir LocalDate a LocalDateTime
            LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
            LocalDateTime fechaFinDateTime = fechaFin.atTime(LocalTime.MAX);

            // Validar y ajustar tipo de venta
            final String tipoVentaFinal = (tipoVenta == null || tipoVenta.isEmpty() || tipoVenta.equals("TODAS")) ? "TODAS" : tipoVenta;
            // Validar y ajustar método de pago
            final String metodoPagoFinal = (metodoPago == null || metodoPago.isEmpty() || metodoPago.equals("TODOS")) ? "TODOS" : metodoPago;

            // Obtener estadísticas de ventas
            Map<String, Double> ventasPorMetodoPago = ventaServicio.obtenerVentasPorMetodoPago(
                licoreriaActual.getId(), fechaInicioDateTime, fechaFinDateTime);
            
            double totalVentas = ventasPorMetodoPago.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

            // Obtener estadísticas de créditos
            Map<String, Double> estadisticasCreditos = creditoServicio.obtenerEstadisticasCreditos(
                licoreriaActual.getId(), fechaInicioDateTime, fechaFinDateTime);

            // Obtener las tasas actuales
            List<PrecioDolar> tasas = precioDolarServicio.obtenerUltimasTasas(licoreriaActual.getId());
            Map<String, Double> tasasMap = new HashMap<>();
            for (PrecioDolar tasa : tasas) {
                tasasMap.put(tasa.getTipoTasa().name().toUpperCase(), tasa.getPrecioDolar());
            }
            // Asegurar que existan las 3 tasas
            tasasMap.putIfAbsent("BCV", 0.0);
            tasasMap.putIfAbsent("PROMEDIO", 0.0);
            tasasMap.putIfAbsent("PARALELA", 0.0);

            // Obtener lista de ventas y calcular subtotales en Bs
            List<Venta> ventas = ventaServicio.listarVentasPorLicoreriaYFecha(
                licoreriaActual.getId(), fechaInicioDateTime, fechaFinDateTime);

            // Filtrar por tipo de venta si es necesario
            if (!"TODAS".equals(tipoVentaFinal)) {
                ventas = ventas.stream()
                    .filter(v -> v.getTipoVenta() == TipoVenta.valueOf(tipoVentaFinal))
                    .toList();
            }
            // Filtrar por método de pago si es necesario
            if (!"TODOS".equals(metodoPagoFinal)) {
                ventas = ventas.stream()
                    .filter(v -> v.getMetodoPago() == MetodoPago.valueOf(metodoPagoFinal))
                    .toList();
            }

            double subtotalDolares = 0.0;
            double subtotalBolivares = 0.0;
            for (Venta venta : ventas) {
                for (DetalleVenta detalle : venta.getDetalles()) {
                    Producto producto = detalle.getProducto();
                    double precioDolar = detalle.getPrecioUnitario().doubleValue();
                    int cantidad = detalle.getCantidad();
                    String tipoTasa = producto.getTipoTasa();
                    double tasa = tasasMap.getOrDefault(tipoTasa != null ? tipoTasa.toUpperCase() : "BCV", 0.0);
                    subtotalDolares += precioDolar * cantidad;
                    subtotalBolivares += precioDolar * cantidad * tasa;
                }
            }

            // Calcular estadísticas adicionales
            int totalProductosVendidos = ventas.stream()
                .flatMap(v -> v.getDetalles().stream())
                .mapToInt(DetalleVenta::getCantidad)
                .sum();

            double ticketPromedio = ventas.isEmpty() ? 0 : 
                totalVentas / ventas.size();

            Map<String, Object> response = new HashMap<>();
            response.put("ventasPorMetodoPago", ventasPorMetodoPago);
            response.put("estadisticasCreditos", estadisticasCreditos);
            response.put("totalVentas", ventas.size());
            response.put("totalIngresos", totalVentas);
            response.put("ticketPromedio", ticketPromedio);
            response.put("productosVendidos", totalProductosVendidos);
            response.put("tasas", tasasMap);
            response.put("subtotalDolares", subtotalDolares);
            response.put("subtotalBolivares", subtotalBolivares);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener datos del dashboard", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al obtener datos: " + e.getMessage()));
        }
    }

    @GetMapping("/lista/data")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> obtenerListaVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "TODAS") String tipoVenta,
            @RequestParam(required = false, defaultValue = "TODOS") String metodoPago,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaVenta,desc") String sort) {
        
        try {
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay licorería seleccionada"));
            }

            // Si no se especifican fechas, usar el mes actual
            LocalDate now = LocalDate.now();
            fechaInicio = fechaInicio != null ? fechaInicio : now.withDayOfMonth(1);
            fechaFin = fechaFin != null ? fechaFin : now;

            // Convertir LocalDate a LocalDateTime
            LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
            LocalDateTime fechaFinDateTime = fechaFin.atTime(LocalTime.MAX);

            // Validar y ajustar tipo de venta
            if (tipoVenta == null || tipoVenta.isEmpty() || tipoVenta.equals("TODAS")) {
                tipoVenta = "TODAS";
            }

            // Validar y ajustar método de pago
            if (metodoPago == null || metodoPago.isEmpty() || metodoPago.equals("TODOS")) {
                metodoPago = "TODOS";
            }

            // Procesar parámetros de ordenamiento
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

            // Crear objeto Pageable
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

            // Obtener página de ventas
            Page<Venta> ventasPage = ventaServicio.listarVentasPaginadas(
                licoreriaActual.getId(),
                fechaInicioDateTime,
                fechaFinDateTime,
                tipoVenta,
                metodoPago,
                pageable
            );

            // Transformar los datos para evitar problemas de serialización
            Map<String, Object> response = new HashMap<>();
            response.put("content", ventasPage.getContent().stream()
                .map(venta -> {
                    Map<String, Object> ventaMap = new HashMap<>();
                    ventaMap.put("id", venta.getId());
                    ventaMap.put("fechaVenta", venta.getFechaVenta());
                    ventaMap.put("totalVenta", venta.getTotalVenta());
                    ventaMap.put("totalVentaBs", venta.getTotalVentaBs());
                    ventaMap.put("metodoPago", venta.getMetodoPago().toString());
                    ventaMap.put("tipoVenta", venta.getTipoVenta().toString());
                    ventaMap.put("cliente", venta.getCliente() != null ? 
                        Map.of(
                            "id", venta.getCliente().getId(),
                            "nombre", venta.getCliente().getNombre(),
                            "apellido", venta.getCliente().getApellido(),
                            "cedula", venta.getCliente().getCedula()
                        ) : null
                    );
                    return ventaMap;
                })
                .collect(Collectors.toList()));
            response.put("totalElements", ventasPage.getTotalElements());
            response.put("totalPages", ventasPage.getTotalPages());
            response.put("size", ventasPage.getSize());
            response.put("number", ventasPage.getNumber());
            response.put("first", ventasPage.isFirst());
            response.put("last", ventasPage.isLast());
            response.put("empty", ventasPage.isEmpty());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener lista de ventas", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al obtener ventas: " + e.getMessage()));
        }
    }

    @GetMapping("/exportar/{formato}")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> exportarReporte(
            @PathVariable String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "TODAS") String tipoVenta,
            @RequestParam(required = false, defaultValue = "TODOS") String metodoPago) {
        
        try {
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay licorería seleccionada"));
            }

            // Si no se especifican fechas, usar el mes actual
            LocalDate now = LocalDate.now();
            fechaInicio = fechaInicio != null ? fechaInicio : now.withDayOfMonth(1);
            fechaFin = fechaFin != null ? fechaFin : now;

            byte[] reporteBytes;
            String nombreArchivo;
            MediaType mediaType;

            if ("pdf".equalsIgnoreCase(formato)) {
                reporteBytes = ventaServicio.generarReportePdf(
                    licoreriaActual.getId(),
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(LocalTime.MAX),
                    tipoVenta,
                    metodoPago
                );
                nombreArchivo = "reporte-ventas.pdf";
                mediaType = MediaType.APPLICATION_PDF;
            } else if ("excel".equalsIgnoreCase(formato)) {
                reporteBytes = ventaServicio.generarReporteExcel(
                    licoreriaActual.getId(),
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(LocalTime.MAX),
                    tipoVenta,
                    metodoPago
                );
                nombreArchivo = "reporte-ventas.xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato no soportado: " + formato));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentDispositionFormData("attachment", nombreArchivo);

            return ResponseEntity.ok()
                .headers(headers)
                .body(reporteBytes);

        } catch (Exception e) {
            logger.error("Error al exportar reporte", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al exportar reporte: " + e.getMessage()));
        }
    }
}