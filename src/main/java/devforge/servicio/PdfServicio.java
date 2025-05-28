package devforge.servicio;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import devforge.model.Venta;
import devforge.model.DetalleVenta;
import devforge.model.ConfiguracionImpresora;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PdfServicio {
    
    public byte[] generarTicketPdf(Venta venta, ConfiguracionImpresora config) {
        try {
            Document document = new Document();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            // Configurar el documento
            document.setPageSize(new Rectangle(226.77f, 800f)); // 80mm x ~300mm
            document.setMargins(10, 10, 10, 10);
            document.open();
            
            // Obtener el formato del ticket
            String formato = (config != null && config.getTicketTexto() != null && !config.getTicketTexto().isBlank())
                ? config.getTicketTexto() 
                : "*** {licoreria} ***\nFecha: {fecha}\n----------------------\n{detalle_productos}\n----------------------\nSUBTOTAL: {subtotal}\nTOTAL: {total}\n¡Gracias por su compra!";
            
            // Reemplazar variables en el formato
            String contenido = formato
                .replace("{licoreria}", venta.getLicoreria().getNombre())
                .replace("{fecha}", venta.getFechaVenta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .replace("{detalle_productos}", generarDetalleProductos(venta))
                .replace("{subtotal}", String.format("%.2f", calcularSubtotal(venta)))
                .replace("{total}", String.format("%.2f", venta.getTotalVenta()));
            
            // Agregar contenido al PDF
            Font font = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL);
            Paragraph paragraph = new Paragraph(contenido, font);
            paragraph.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }
    
    private String generarDetalleProductos(Venta venta) {
        StringBuilder detalle = new StringBuilder();
        venta.getDetalles().forEach(detalleVenta -> {
            detalle.append(String.format("%s\n", detalleVenta.getProducto().getNombre()));
            detalle.append(String.format("%d x %.2f = %.2f\n", 
                detalleVenta.getCantidad(),
                detalleVenta.getPrecioUnitario(),
                detalleVenta.getSubtotal()));
        });
        return detalle.toString();
    }

    private BigDecimal calcularSubtotal(Venta venta) {
        return venta.getDetalles().stream()
            .map(DetalleVenta::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
} 