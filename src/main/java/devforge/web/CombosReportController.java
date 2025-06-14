package devforge.web;

import devforge.servicio.VentaComboService;
import devforge.config.LicoreriaContext;
import devforge.model.VentaCombo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

// PDF imports
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

@Controller
public class CombosReportController {
    @Autowired
    private VentaComboService ventaComboService;
    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping("/reportes/combos")
    public String dashboardCombos() {
        return "reportes/combos";
    }

    @GetMapping("/api/reportes/combos/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        var licoreria = licoreriaContext.getLicoreriaActual();
        if (licoreria == null) return ResponseEntity.badRequest().body(null);
        List<VentaCombo> ventas;
        if (inicio != null && fin != null) {
            ventas = ventaComboService.listarPorLicoreriaYFecha(
                licoreria.getId(),
                inicio.atStartOfDay(),
                fin.atTime(23,59,59)
            );
        } else {
            ventas = ventaComboService.listarPorLicoreria(licoreria.getId());
        }
        // Generar PDF con OpenPDF
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font cellFont = new Font(Font.HELVETICA, 11);
            document.add(new Paragraph("Reporte de Ventas de Combos", titleFont));
            document.add(new Paragraph("\n"));
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{18, 32, 12, 18, 20, 20});
            table.addCell(new PdfPCell(new Phrase("Fecha", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Combo", headerFont)));
            table.addCell(new PdfPCell(new Phrase("USD", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Bs", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Método Pago", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Ticket Promedio", headerFont)));
            double totalUSD = 0;
            double totalBs = 0;
            for (VentaCombo v : ventas) {
                String fecha = v.getFechaVenta() != null ? v.getFechaVenta().toLocalDate().toString() : "";
                String combo = v.getCombo() != null ? v.getCombo().getNombre() : "";
                String usd = v.getValorVentaUSD() != null ? "$" + v.getValorVentaUSD().toString() : "$0.00";
                String bs = v.getValorVentaBS() != null ? v.getValorVentaBS().toString() + " Bs" : "0.00 Bs";
                String metodo = v.getMetodoPago() != null ? v.getMetodoPago() : "";
                table.addCell(new PdfPCell(new Phrase(fecha, cellFont)));
                table.addCell(new PdfPCell(new Phrase(combo, cellFont)));
                table.addCell(new PdfPCell(new Phrase(usd, cellFont)));
                table.addCell(new PdfPCell(new Phrase(bs, cellFont)));
                table.addCell(new PdfPCell(new Phrase(metodo, cellFont)));
                table.addCell(new PdfPCell(new Phrase("-", cellFont)));
                try {
                    totalUSD += v.getValorVentaUSD() != null ? v.getValorVentaUSD().doubleValue() : 0;
                    totalBs += v.getValorVentaBS() != null ? v.getValorVentaBS().doubleValue() : 0;
                } catch(Exception ignore) {}
            }
            // Totales
            PdfPCell totalCell = new PdfPCell(new Phrase("Totales", headerFont));
            totalCell.setColspan(2);
            table.addCell(totalCell);
            table.addCell(new PdfPCell(new Phrase("$" + String.format("%.2f", totalUSD), headerFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f Bs", totalBs), headerFont)));
            table.addCell(new PdfPCell(new Phrase("", headerFont)));
            table.addCell(new PdfPCell(new Phrase("", headerFont)));
            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] pdfBytes = baos.toByteArray();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ReporteCombos.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }
}
