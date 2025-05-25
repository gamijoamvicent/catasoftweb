package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.UsuarioServicio;
import devforge.servicio.VentaServicio;
import devforge.servicio.CreditoServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;

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

        // Obtener estadísticas de créditos
        Map<String, Double> estadisticasCreditos = creditoServicio.obtenerEstadisticasCreditos(
            licoreriaActual.getId(), fechaInicioDateTime, fechaFinDateTime);

        model.addAttribute("licoreriaActual", licoreriaActual);
        model.addAttribute("usuario", usuario);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("ventasPorMetodoPago", ventasPorMetodoPago);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("estadisticasCreditos", estadisticasCreditos);

        return "reportes/ventas";
    }

    @GetMapping("/exportar/pdf")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<Resource> exportarPDF(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin) {
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            
            document.open();
            
            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Reporte de Ventas", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            // Fechas
            document.add(new Paragraph("\nPeriodo: " + fechaInicio + " a " + fechaFin));
            
            // Tabla de ventas
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(20f);
            
            // Encabezados
            Stream.of("Método de Pago", "Total", "Porcentaje")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });
                
            // Datos
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            Map<String, Double> ventasPorMetodo = ventaServicio.obtenerVentasPorMetodoPago(
                licoreriaActual.getId(),
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX));
                
            double total = ventasPorMetodo.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
                
            ventasPorMetodo.forEach((metodo, monto) -> {
                table.addCell(metodo);
                table.addCell(String.format("$%.2f", monto));
                table.addCell(String.format("%.1f%%", (monto/total)*100));
            });
            
            document.add(table);
            document.close();
            
            ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "inline; filename=reporte-ventas.pdf");
            
            return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
                
        } catch (Exception e) {
            logger.error("Error al generar PDF", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/exportar/excel")
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin) {
        try {
            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();
            
            // Si no se especifican fechas, usar el mes actual
            if (fechaInicio == null) {
                fechaInicio = LocalDate.now().withDayOfMonth(1);
            }
            if (fechaFin == null) {
                fechaFin = LocalDate.now();
            }
            
            // Obtener datos
            Map<String, Double> ventasPorMetodo = ventaServicio.obtenerVentasPorMetodoPago(
                licoreriaActual.getId(),
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX));
            
            Map<String, Double> estadisticasCreditos = creditoServicio.obtenerEstadisticasCreditos(
                licoreriaActual.getId(),
                fechaInicio.atStartOfDay(),
                fechaFin.atTime(LocalTime.MAX));

            // Crear workbook
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Reporte de Ventas");

                // Estilos
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                // Título
                Row titleRow = sheet.createRow(0);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("Reporte de Ventas - " + licoreriaActual.getNombre());
                titleCell.setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

                // Período
                Row periodRow = sheet.createRow(1);
                periodRow.createCell(0).setCellValue("Período: " + fechaInicio + " a " + fechaFin);
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

                // Ventas por método de pago
                Row ventasHeader = sheet.createRow(3);
                ventasHeader.createCell(0).setCellValue("Ventas por Método de Pago");
                ventasHeader.getCell(0).setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 2));

                Row metodosHeader = sheet.createRow(4);
                metodosHeader.createCell(0).setCellValue("Método de Pago");
                metodosHeader.createCell(1).setCellValue("Monto");
                metodosHeader.getCell(0).setCellStyle(headerStyle);
                metodosHeader.getCell(1).setCellStyle(headerStyle);

                int rowNum = 5;
                for (Map.Entry<String, Double> entry : ventasPorMetodo.entrySet()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(entry.getValue());
                }

                // Estadísticas de créditos
                Row creditosHeader = sheet.createRow(rowNum + 1);
                creditosHeader.createCell(0).setCellValue("Estadísticas de Créditos");
                creditosHeader.getCell(0).setCellStyle(headerStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, 0, 2));

                Row creditosLabelsHeader = sheet.createRow(rowNum + 2);
                creditosLabelsHeader.createCell(0).setCellValue("Concepto");
                creditosLabelsHeader.createCell(1).setCellValue("Monto");
                creditosLabelsHeader.getCell(0).setCellStyle(headerStyle);
                creditosLabelsHeader.getCell(1).setCellStyle(headerStyle);

                rowNum += 3;
                for (Map.Entry<String, Double> entry : estadisticasCreditos.entrySet()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(entry.getKey());
                    row.createCell(1).setCellValue(entry.getValue());
                }

                // Ajustar ancho de columnas
                for (int i = 0; i < 2; i++) {
                    sheet.autoSizeColumn(i);
                }

                // Escribir a ByteArrayOutputStream
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                byte[] bytes = outputStream.toByteArray();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDisposition(ContentDisposition.attachment().filename("reporte-ventas.xlsx").build());
                headers.setContentLength(bytes.length);

                return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("Error al generar Excel", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 