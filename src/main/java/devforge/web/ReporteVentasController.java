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
import devforge.web.dto.VentaCajaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import java.util.TreeMap;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Comparator;

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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import devforge.servicio.VentaCajaServicio;

@Controller
@RequestMapping("/reportes")
public class ReporteVentasController {
    private static final Logger logger = LoggerFactory.getLogger(ReporteVentasController.class);

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private VentaCajaServicio ventaCajaServicio;

    @Autowired
    private VentaServicio ventaServicio;

    @Autowired
    private CreditoServicio creditoServicio;

    @Autowired
    private PrecioDolarServicio precioDolarServicio;

    @Autowired
    private ProductoServicio productoServicio;

    @GetMapping("/ventas")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarDashboard(Model model) {
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        if (licoreriaActual == null) {
            return "redirect:/error?mensaje=No hay licorería seleccionada";
        }

        model.addAttribute("licoreriaActual", licoreriaActual);
        return "reportes/dashboard";
    }

    @GetMapping("/ventas-cajas")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarConfiguracionCajas(Model model) {
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        if (licoreriaActual == null) {
            return "redirect:/error?mensaje=No hay licorería seleccionada";
        }

        model.addAttribute("licoreriaActual", licoreriaActual);
        return "reportes/ventas-cajas";
    }

    @GetMapping("/ventas-cajas/data")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> obtenerDatosCajasDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "TODAS") String tipoCaja) {

        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            Long licoreriaId = licoreriaContext.getLicoreriaActual().getId();

            // Si no se proporcionan fechas, usar el último mes como predeterminado
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().minusMonths(1);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now();
            }

            // Convertir fechas a LocalDateTime para consultas
            LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
            LocalDateTime fechaFinDateTime = fechaFin.atTime(23, 59, 59);

            // Obtener datos del servicio
            List<VentaCajaDTO> ventasCajas = ventaCajaServicio.buscarVentasCajasPorFechaYTipo(
                    licoreriaId, fechaInicioDateTime, fechaFinDateTime, tipoCaja);

            // Calcular estadísticas
            Map<String, Object> estadisticas = calcularEstadisticasCajas(ventasCajas);

            // Obtener ventas por tipo de caja
            List<Map<String, Object>> ventasPorTipo = obtenerVentasPorTipoCaja(ventasCajas);

            // Obtener tendencia de ventas por día
            List<Map<String, Object>> tendencia = obtenerTendenciaVentasCajas(ventasCajas);

            // Obtener top 5 cajas más vendidas
            List<Map<String, Object>> topCajas = obtenerTopCajasMasVendidas(ventasCajas, 5);

            // Obtener ingresos por día
            List<Map<String, Object>> ingresosPorDia = obtenerIngresosPorDia(ventasCajas);

            // Obtener últimas ventas para la tabla
            List<Map<String, Object>> ultimasVentas = obtenerUltimasVentasCajas(ventasCajas, 10);

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("estadisticas", estadisticas);
            respuesta.put("ventasPorTipo", ventasPorTipo);
            respuesta.put("tendencia", tendencia);
            respuesta.put("topCajas", topCajas);
            respuesta.put("ingresosPorDia", ingresosPorDia);
            respuesta.put("ultimasVentas", ultimasVentas);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            logger.error("Error al obtener datos del dashboard de cajas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cargar los datos: " + e.getMessage()));
        }
    }

    @GetMapping("/ventas/lista")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String mostrarListaVentas(Model model) {
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
        if (licoreriaActual == null) {
            return "redirect:/error?mensaje=No hay licorería seleccionada";
        }

        model.addAttribute("licoreriaActual", licoreriaActual);
        return "reportes/lista";
    }

    @GetMapping("/ventas-cajas/exportar")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> exportarReporteCajas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "TODAS") String tipoCaja) {

        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            Long licoreriaId = licoreriaContext.getLicoreriaActual().getId();

            // Si no se proporcionan fechas, usar el último mes como predeterminado
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().minusMonths(1);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now();
            }

            // Convertir fechas a LocalDateTime para consultas
            LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
            LocalDateTime fechaFinDateTime = fechaFin.atTime(23, 59, 59);

            // Obtener datos del servicio
            List<VentaCajaDTO> ventasCajas = ventaCajaServicio.buscarVentasCajasPorFechaYTipo(
                    licoreriaId, fechaInicioDateTime, fechaFinDateTime, tipoCaja);

            // Crear archivo Excel
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Ventas de Cajas");

            // Crear estilos
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Crear cabecera
            String[] columns = {"Fecha", "Tipo de Caja", "Nombre de Caja", "Cantidad", "Precio Unitario", "Subtotal", "Método de Pago", "Cliente"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (VentaCajaDTO venta : ventasCajas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(venta.getFechaCreacion().format(formatter));
                row.createCell(1).setCellValue(venta.getTipoCaja());
                row.createCell(2).setCellValue(venta.getCajaNombre());
                row.createCell(3).setCellValue(venta.getCantidad());
                row.createCell(4).setCellValue(venta.getPrecioUnitario().doubleValue());
                row.createCell(5).setCellValue(venta.getSubtotal().doubleValue());
                row.createCell(6).setCellValue(venta.getMetodoPago() != null ? venta.getMetodoPago() : "");
                row.createCell(7).setCellValue(venta.getNombreCliente() != null ? venta.getNombreCliente() : "");
            }

            // Ajustar ancho de columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Escribir al output stream
            workbook.write(out);
            workbook.close();

            // Configurar respuesta
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ventas-cajas.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error al exportar reporte de ventas de cajas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al exportar el reporte: " + e.getMessage());
        }
    }

    @GetMapping("/ventas/data")
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

            // Obtener lista de ventas y calcular subtotales en Bs
            List<Venta> ventas = ventaServicio.listarVentasPorLicoreriaYFecha(
                licoreriaActual.getId(), fechaInicioDateTime, fechaFinDateTime);
            
            // Filtrar ventas anuladas
            ventas = ventas.stream()
                .filter(v -> !v.isAnulada())
                .collect(Collectors.toList());

            // Filtrar por tipo de venta si es necesario
            if (!"TODAS".equals(tipoVentaFinal)) {
                ventas = ventas.stream()
                    .filter(v -> v.getTipoVenta() == TipoVenta.valueOf(tipoVentaFinal))
                    .collect(Collectors.toList());
            }
            // Filtrar por método de pago si es necesario
            if (!"TODOS".equals(metodoPagoFinal)) {
                ventas = ventas.stream()
                    .filter(v -> v.getMetodoPago() == MetodoPago.valueOf(metodoPagoFinal))
                    .collect(Collectors.toList());
            }

            // Calcular totales
            double subtotalDolares = ventas.stream()
                .mapToDouble(v -> v.getTotalVenta().doubleValue())
                .sum();

            double subtotalBolivares = ventas.stream()
                .mapToDouble(v -> v.getTotalVentaBs() != null ? v.getTotalVentaBs().doubleValue() : 0.0)
                .sum();

            // Calcular total de ventas y productos vendidos
            int totalVentas = ventas.size();
            int productosVendidos = ventas.stream()
                .mapToInt(v -> v.getDetalles().stream()
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum())
                .sum();

            // Calcular ticket promedio
            double ticketPromedio = totalVentas > 0 ? subtotalDolares / totalVentas : 0.0;

            // Obtener estadísticas de ventas por método de pago
            Map<String, Double> ventasPorMetodoPago = ventas.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getMetodoPago().toString(),
                    Collectors.summingDouble(v -> v.getTotalVenta().doubleValue())
                ));

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

            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("subtotalDolares", subtotalDolares);
            response.put("subtotalBolivares", subtotalBolivares);
            response.put("totalVentas", totalVentas);
            response.put("totalIngresos", subtotalDolares);
            response.put("ticketPromedio", ticketPromedio);
            response.put("productosVendidos", productosVendidos);
            response.put("ventasPorMetodoPago", ventasPorMetodoPago);
            response.put("estadisticasCreditos", estadisticasCreditos);
            response.put("tasas", tasasMap);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al obtener datos del dashboard", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al obtener datos: " + e.getMessage()));
        }
    }

    @GetMapping("/ventas/lista/data")
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

            // Obtener página de ventas (excluyendo las anuladas)
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
                .filter(venta -> !venta.isAnulada()) // Filtrar ventas anuladas
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

    @GetMapping("/ventas/exportar/{formato}")
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

    @GetMapping("/ventas/pdf/{ventaId}")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> generarPDFVenta(@PathVariable Long ventaId) {
        try {
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay licorería seleccionada"));
            }

            Optional<Venta> ventaOpt = ventaServicio.buscarPorId(ventaId);
            if (ventaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Venta venta = ventaOpt.get();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();

            // Configurar fuentes
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Título
            Paragraph title = new Paragraph("Detalle de Venta", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Información de la venta
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(10);
            infoTable.setSpacingAfter(10);

            addTableRow(infoTable, "Número de Venta:", "#" + venta.getId(), headerFont, normalFont);
            addTableRow(infoTable, "Fecha:", venta.getFechaVenta().toString(), headerFont, normalFont);
            addTableRow(infoTable, "Cliente:", venta.getCliente() != null ? venta.getCliente().getNombre() : "Venta al contado", headerFont, normalFont);
            addTableRow(infoTable, "Tipo de Venta:", venta.getTipoVenta().toString(), headerFont, normalFont);
            addTableRow(infoTable, "Método de Pago:", venta.getMetodoPago().toString(), headerFont, normalFont);

            document.add(infoTable);

            // Tabla de productos
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(10);

            // Encabezados
            String[] headers = {"Producto", "Cantidad", "Precio Unit.", "Subtotal"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Detalles de la venta
            for (DetalleVenta detalle : venta.getDetalles()) {
                table.addCell(new Phrase(detalle.getProducto().getNombre(), normalFont));
                table.addCell(new Phrase(String.valueOf(detalle.getCantidad()), normalFont));
                table.addCell(new Phrase(String.format("%.2f", detalle.getPrecioUnitario().doubleValue()), normalFont));
                table.addCell(new Phrase(String.format("%.2f", detalle.getSubtotal().doubleValue()), normalFont));
            }

            document.add(table);

            // Totales
            PdfPTable totalesTable = new PdfPTable(2);
            totalesTable.setWidthPercentage(100);
            totalesTable.setSpacingBefore(10);

            addTableRow(totalesTable, "Total en USD:", String.format("%.2f", new BigDecimal(venta.getTotalVenta().toString()).doubleValue()), headerFont, normalFont);
            addTableRow(totalesTable, "Total en Bs:", String.format("%.2f Bs", new BigDecimal(venta.getTotalVentaBs().toString()).doubleValue()), headerFont, normalFont);

            document.add(totalesTable);

            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("attachment", "venta-" + ventaId + ".pdf");

            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(baos.toByteArray());

        } catch (Exception e) {
            logger.error("Error al generar PDF de venta", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al generar PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/ventas/ticket/{ventaId}")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> generarTicketVenta(@PathVariable Long ventaId) {
        try {
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay licorería seleccionada"));
            }

            Optional<Venta> ventaOpt = ventaServicio.buscarPorId(ventaId);
            if (ventaOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Venta venta = ventaOpt.get();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);

            document.open();

            // Configurar fuentes
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // Encabezado
            Paragraph title = new Paragraph(licoreriaActual.getNombre(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Paragraph subtitle = new Paragraph("Ticket de Venta", headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            // Información de la venta
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(5);
            infoTable.setSpacingAfter(5);

            addTableRow(infoTable, "Venta #:", String.valueOf(venta.getId()), headerFont, normalFont);
            addTableRow(infoTable, "Fecha:", venta.getFechaVenta().toString(), headerFont, normalFont);
            addTableRow(infoTable, "Cliente:", venta.getCliente() != null ? venta.getCliente().getNombre() : "Venta al contado", headerFont, normalFont);

            document.add(infoTable);

            // Línea separadora
            document.add(new Paragraph("----------------------------------------"));

            // Tabla de productos
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);
            table.setSpacingAfter(5);

            // Encabezados
            String[] headers = {"Producto", "Cant.", "P.Unit.", "Subtotal"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Detalles de la venta
            for (DetalleVenta detalle : venta.getDetalles()) {
                table.addCell(new Phrase(detalle.getProducto().getNombre(), normalFont));
                table.addCell(new Phrase(String.valueOf(detalle.getCantidad()), normalFont));
                table.addCell(new Phrase(String.format("%.2f", detalle.getPrecioUnitario().doubleValue()), normalFont));
                table.addCell(new Phrase(String.format("%.2f", detalle.getSubtotal().doubleValue()), normalFont));
            }

            document.add(table);

            // Línea separadora
            document.add(new Paragraph("----------------------------------------"));

            // Totales
            PdfPTable totalesTable = new PdfPTable(2);
            totalesTable.setWidthPercentage(100);
            totalesTable.setSpacingBefore(5);

            addTableRow(totalesTable, "Total USD:", String.format("%.2f", new BigDecimal(venta.getTotalVenta().toString()).doubleValue()), headerFont, normalFont);
            addTableRow(totalesTable, "Total Bs:", String.format("%.2f Bs", new BigDecimal(venta.getTotalVentaBs().toString()).doubleValue()), headerFont, normalFont);

            document.add(totalesTable);

            // Pie de página
            Paragraph footer = new Paragraph("¡Gracias por su compra!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(10);
            document.add(footer);

            document.close();

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.setContentDispositionFormData("inline", "ticket-" + ventaId + ".pdf");

            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(baos.toByteArray());

        } catch (Exception e) {
            logger.error("Error al generar ticket de venta", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al generar ticket: " + e.getMessage()));
        }
    }

    @PostMapping("/ventas/{ventaId}/anular")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> anularVenta(
            @PathVariable Long ventaId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            logger.info("Iniciando anulación de venta ID: {}", ventaId);
            
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            if (licoreriaActual == null) {
                logger.error("No hay licorería seleccionada");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No hay licorería seleccionada"));
            }

            // Obtener la venta
            Optional<Venta> ventaOpt = ventaServicio.buscarPorId(ventaId);
            if (ventaOpt.isEmpty()) {
                logger.error("Venta no encontrada con ID: {}", ventaId);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Venta no encontrada"));
            }
            Venta venta = ventaOpt.get();

            // Verificar que la venta pertenece a la licorería actual
            if (!venta.getLicoreriaId().equals(licoreriaActual.getId())) {
                logger.error("Intento de anular venta de otra licorería. Venta ID: {}, Licorería actual: {}", 
                    ventaId, licoreriaActual.getId());
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "No tiene permiso para anular esta venta"));
            }

            // Verificar que la venta no esté ya anulada
            if (venta.isAnulada()) {
                logger.error("Intento de anular una venta ya anulada. Venta ID: {}", ventaId);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Esta venta ya está anulada"));
            }

            // Obtener el usuario actual
            String username = authentication.getName();
            Usuario usuario = usuarioServicio.buscarPorUsername(username);
            if (usuario == null) {
                logger.error("Usuario no encontrado: {}", username);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Usuario no encontrado"));
            }

            // Obtener el motivo de anulación
            String motivo = request.get("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                logger.error("Intento de anular venta sin motivo. Venta ID: {}", ventaId);
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe proporcionar un motivo para la anulación"));
            }

            try {
                logger.info("Procediendo a anular venta ID: {} por usuario: {}", ventaId, username);
                // Anular la venta
                ventaServicio.anularVenta(ventaId, usuario.getId(), motivo);
                logger.info("Venta ID: {} anulada exitosamente", ventaId);
                return ResponseEntity.ok(Map.of("message", "Venta anulada exitosamente"));
            } catch (RuntimeException e) {
                logger.error("Error al anular venta ID: {}. Error: {}", ventaId, e.getMessage(), e);
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Error desconocido al anular la venta";
                return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
            }
        } catch (Exception e) {
            logger.error("Error inesperado al anular venta ID: {}. Error: {}", ventaId, e.getMessage(), e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Error inesperado al anular la venta";
            return ResponseEntity.internalServerError()
                .body(Map.of("error", errorMessage));
        }
    }

    private void addTableRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(0);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(0);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("$%.2f", amount.doubleValue());
    }

    private String formatCurrencyBs(BigDecimal amount) {
        return String.format("%.2f Bs", amount.doubleValue());
    }

    private Map<String, Object> calcularEstadisticasCajas(List<VentaCajaDTO> ventasCajas) {
        Map<String, Object> estadisticas = new HashMap<>();

        if (ventasCajas.isEmpty()) {
            estadisticas.put("totalCajas", 0);
            estadisticas.put("totalIngresos", 0.0);
            estadisticas.put("promedioVenta", 0.0);
            estadisticas.put("cajaMasVendida", "-");
            estadisticas.put("cajaMasVendidaCantidad", 0);
            return estadisticas;
        }

        // Total de cajas vendidas
        int totalCajas = ventasCajas.stream().mapToInt(VentaCajaDTO::getCantidad).sum();

        // Total de ingresos
        BigDecimal totalIngresos = ventasCajas.stream()
                .map(VentaCajaDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Promedio por venta
        BigDecimal promedioVenta = totalIngresos.divide(BigDecimal.valueOf(ventasCajas.size()), 2, RoundingMode.HALF_UP);

        // Caja más vendida
        Map<String, Integer> cantidadPorCaja = new HashMap<>();
        for (VentaCajaDTO venta : ventasCajas) {
            cantidadPorCaja.merge(venta.getCajaNombre(), venta.getCantidad(), Integer::sum);
        }

        String cajaMasVendida = "-";
        int cantidadMasVendida = 0;

        for (Map.Entry<String, Integer> entry : cantidadPorCaja.entrySet()) {
            if (entry.getValue() > cantidadMasVendida) {
                cajaMasVendida = entry.getKey();
                cantidadMasVendida = entry.getValue();
            }
        }

        estadisticas.put("totalCajas", totalCajas);
        estadisticas.put("totalIngresos", totalIngresos);
        estadisticas.put("promedioVenta", promedioVenta);
        estadisticas.put("cajaMasVendida", cajaMasVendida);
        estadisticas.put("cajaMasVendidaCantidad", cantidadMasVendida);

        return estadisticas;
    }

    private List<Map<String, Object>> obtenerVentasPorTipoCaja(List<VentaCajaDTO> ventasCajas) {
        Map<String, Integer> cantidadPorTipo = new HashMap<>();

        for (VentaCajaDTO venta : ventasCajas) {
            cantidadPorTipo.merge(venta.getTipoCaja(), venta.getCantidad(), Integer::sum);
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cantidadPorTipo.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("tipo", entry.getKey());
            item.put("cantidad", entry.getValue());
            resultado.add(item);
        }

        return resultado;
    }

    private List<Map<String, Object>> obtenerTendenciaVentasCajas(List<VentaCajaDTO> ventasCajas) {
        // Agrupar ventas por fecha (día)
        Map<LocalDate, Integer> ventasPorDia = new TreeMap<>();

        for (VentaCajaDTO venta : ventasCajas) {
            LocalDate fecha = venta.getFechaCreacion().toLocalDate();
            ventasPorDia.merge(fecha, venta.getCantidad(), Integer::sum);
        }

        // Convertir a formato de respuesta
        List<Map<String, Object>> resultado = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Map.Entry<LocalDate, Integer> entry : ventasPorDia.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("fecha", entry.getKey().format(formatter));
            item.put("cantidad", entry.getValue());
            resultado.add(item);
        }

        return resultado;
    }

    private List<Map<String, Object>> obtenerTopCajasMasVendidas(List<VentaCajaDTO> ventasCajas, int limit) {
        // Agrupar por nombre de caja
        Map<String, Integer> cantidadPorCaja = new HashMap<>();

        for (VentaCajaDTO venta : ventasCajas) {
            cantidadPorCaja.merge(venta.getCajaNombre(), venta.getCantidad(), Integer::sum);
        }

        // Ordenar por cantidad y limitar al top solicitado
        List<Map.Entry<String, Integer>> sortedEntries = cantidadPorCaja.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Convertir a formato de respuesta
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            Map<String, Object> item = new HashMap<>();
            item.put("nombre", entry.getKey());
            item.put("cantidad", entry.getValue());
            resultado.add(item);
        }

        return resultado;
    }

    private List<Map<String, Object>> obtenerIngresosPorDia(List<VentaCajaDTO> ventasCajas) {
        // Agrupar ingresos por fecha (día)
        Map<LocalDate, BigDecimal> ingresosPorDia = new TreeMap<>();

        for (VentaCajaDTO venta : ventasCajas) {
            LocalDate fecha = venta.getFechaCreacion().toLocalDate();
            ingresosPorDia.merge(fecha, venta.getSubtotal(), BigDecimal::add);
        }

        // Convertir a formato de respuesta
        List<Map<String, Object>> resultado = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Map.Entry<LocalDate, BigDecimal> entry : ingresosPorDia.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("fecha", entry.getKey().format(formatter));
            item.put("total", entry.getValue());
            resultado.add(item);
        }

        return resultado;
    }

    private List<Map<String, Object>> obtenerUltimasVentasCajas(List<VentaCajaDTO> ventasCajas, int limit) {
        // Ordenar por fecha de creación (más recientes primero) y limitar
        List<VentaCajaDTO> ultimasVentas = ventasCajas.stream()
                .sorted(Comparator.comparing(VentaCajaDTO::getFechaCreacion).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // Convertir a formato de respuesta
        List<Map<String, Object>> resultado = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (VentaCajaDTO venta : ultimasVentas) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", venta.getId());
            item.put("fecha", venta.getFechaCreacion().format(formatter));
            item.put("tipoCaja", venta.getTipoCaja());
            item.put("cajaNombre", venta.getCajaNombre());
            item.put("cantidad", venta.getCantidad());
            item.put("precioUnitario", venta.getPrecioUnitario());
            item.put("total", venta.getSubtotal());
            resultado.add(item);
        }

        return resultado;
    }

    // Métodos de redirección para mantener compatibilidad con URLs antiguas
    @GetMapping("/lista")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public String redirigirListaVentas() {
        return "redirect:/reportes/ventas/lista";
    }
}