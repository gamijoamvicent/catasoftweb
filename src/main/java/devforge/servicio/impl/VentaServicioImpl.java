package devforge.servicio.impl;

import devforge.model.Venta;
import devforge.model.enums.TipoVenta;
import devforge.model.enums.MetodoPago;
import devforge.repository.VentaRepository;
import devforge.servicio.VentaServicio;
import devforge.servicio.UsuarioServicio;
import devforge.servicio.CreditoServicio;
import devforge.model.Cliente;
import devforge.servicio.ClienteServicio;
import devforge.model.Usuario;
import devforge.model.DetalleVenta;
import devforge.model.Producto;
import devforge.servicio.ProductoServicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.BaseColor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class VentaServicioImpl implements VentaServicio {

    private final VentaRepository ventaRepository;
    private final UsuarioServicio usuarioServicio;
    private final CreditoServicio creditoServicio;
    private final ClienteServicio clienteServicio;
    private final ProductoServicio productoServicio;
    private static final Logger logger = LoggerFactory.getLogger(VentaServicioImpl.class);

    public VentaServicioImpl(VentaRepository ventaRepository, 
                            UsuarioServicio usuarioServicio,
                            CreditoServicio creditoServicio,
                            ClienteServicio clienteServicio,
                            ProductoServicio productoServicio) {
        this.ventaRepository = ventaRepository;
        this.usuarioServicio = usuarioServicio;
        this.creditoServicio = creditoServicio;
        this.clienteServicio = clienteServicio;
        this.productoServicio = productoServicio;
    }

    @Override
    public Venta guardar(Venta venta) {
        return ventaRepository.save(venta);
    }

    @Override
    public List<Venta> listarVentasPorLicoreria(Long licoreriaId) {
        return ventaRepository.findByLicoreriaId(licoreriaId);
    }

    @Override
    public List<Venta> listarVentasPorLicoreriaYFecha(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ventaRepository.findByLicoreriaIdAndFechaVentaBetween(licoreriaId, fechaInicio, fechaFin);
    }

    @Override
    public Optional<Venta> buscarPorId(Long id) {
        return ventaRepository.findById(id);
    }

    @Override
    public Map<String, Double> obtenerVentasPorMetodoPago(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = listarVentasPorLicoreriaYFecha(licoreriaId, fechaInicio, fechaFin);
        return ventas.stream()
            .filter(v -> !v.isAnulada())
            .collect(Collectors.groupingBy(
                venta -> venta.getMetodoPago().toString(),
                Collectors.summingDouble(v -> v.getTotalVenta().doubleValue())
            ));
    }

    @Override
    public Page<Venta> listarVentasPaginadas(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago,
            Pageable pageable) {
        
        Specification<Venta> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filtro por licorería
            predicates.add(cb.equal(root.get("licoreria").get("id"), licoreriaId));
            
            // Filtro por rango de fechas
            predicates.add(cb.between(root.get("fechaVenta"), fechaInicio, fechaFin));
            
            // Filtro por tipo de venta
            if (!"TODAS".equals(tipoVenta)) {
                predicates.add(cb.equal(root.get("tipoVenta"), TipoVenta.valueOf(tipoVenta)));
            }
            
            // Filtro por método de pago
            if (!"TODOS".equals(metodoPago)) {
                predicates.add(cb.equal(root.get("metodoPago"), MetodoPago.valueOf(metodoPago)));
            }

            // Excluir ventas anuladas
            predicates.add(cb.equal(root.get("anulada"), false));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return ventaRepository.findAll(spec, pageable);
    }

    private void agregarContenidoPdf(Document document, List<Venta> ventas, 
            LocalDateTime fechaInicio, LocalDateTime fechaFin,
            String tipoVenta, String metodoPago) throws DocumentException {
        // Título del reporte
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Reporte de Ventas", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));
        
        // Información del filtro
        document.add(new Paragraph("Período: " + fechaInicio.toLocalDate() + " - " + fechaFin.toLocalDate()));
        document.add(new Paragraph("Tipo de Venta: " + tipoVenta));
        document.add(new Paragraph("Método de Pago: " + metodoPago));
        document.add(new Paragraph("\n"));
        
        // Tabla de ventas
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        
        // Encabezados
        Stream.of("Fecha", "Nro. Venta", "Cliente", "Tipo", "Método Pago", "Total USD", "Total Bs")
            .forEach(columnTitle -> {
                PdfPCell header = new PdfPCell();
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                header.setBorderWidth(2);
                header.setPhrase(new Phrase(columnTitle));
                table.addCell(header);
            });
        
        // Datos
        ventas.forEach(venta -> {
            table.addCell(venta.getFechaVenta().toString());
            table.addCell(String.valueOf(venta.getId()));
            table.addCell(venta.getCliente() != null ? venta.getCliente().getNombre() : "Venta al contado");
            table.addCell(venta.getTipoVenta().toString());
            table.addCell(venta.getMetodoPago().toString());
            table.addCell(String.format("$%.2f", venta.getTotalVenta()));
            table.addCell(String.format("%.2f Bs", venta.getTotalVentaBs() != null ? 
                venta.getTotalVentaBs() : BigDecimal.ZERO));
        });
        
        document.add(table);
    }

    @Override
    public byte[] generarReportePdf(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago) throws IOException {
        
        // Obtener las ventas para el reporte
        List<Venta> ventas = listarVentasFiltradas(licoreriaId, fechaInicio, fechaFin, tipoVenta, metodoPago);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            agregarContenidoPdf(document, ventas, fechaInicio, fechaFin, tipoVenta, metodoPago);
        } catch (DocumentException e) {
            throw new IOException("Error al generar el PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }
        
        return baos.toByteArray();
    }

    @Override
    public byte[] generarReporteExcel(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago) throws IOException {
        
        // Obtener las ventas para el reporte
        List<Venta> ventas = listarVentasFiltradas(licoreriaId, fechaInicio, fechaFin, tipoVenta, metodoPago);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Ventas");
            
            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Encabezados
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Fecha", "Nro. Venta", "Cliente", "Tipo", "Método Pago", "Total USD", "Total Bs"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Datos
            int rowNum = 1;
            for (Venta venta : ventas) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(venta.getFechaVenta().toString());
                row.createCell(1).setCellValue(venta.getId());
                row.createCell(2).setCellValue(venta.getCliente() != null ? 
                    venta.getCliente().getNombre() : "Venta al contado");
                row.createCell(3).setCellValue(venta.getTipoVenta().toString());
                row.createCell(4).setCellValue(venta.getMetodoPago().toString());
                row.createCell(5).setCellValue(venta.getTotalVenta().doubleValue());
                row.createCell(6).setCellValue(
                    venta.getTotalVentaBs() != null ? venta.getTotalVentaBs().doubleValue() : 0.0);
            }
            
            // Autoajustar columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private List<Venta> listarVentasFiltradas(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoVenta,
            String metodoPago) {
        
        Specification<Venta> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(cb.equal(root.get("licoreria").get("id"), licoreriaId));
            predicates.add(cb.between(root.get("fechaVenta"), fechaInicio, fechaFin));
            
            if (!"TODAS".equals(tipoVenta)) {
                predicates.add(cb.equal(root.get("tipoVenta"), TipoVenta.valueOf(tipoVenta)));
            }
            
            if (!"TODOS".equals(metodoPago)) {
                predicates.add(cb.equal(root.get("metodoPago"), MetodoPago.valueOf(metodoPago)));
            }

            // Excluir ventas anuladas
            predicates.add(cb.equal(root.get("anulada"), false));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return ventaRepository.findAll(spec);
    }

    @Override
    @Transactional
    public void anularVenta(Long ventaId, Long usuarioId, String motivo) {
        try {
            Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

            if (venta.isAnulada()) {
                throw new RuntimeException("La venta ya está anulada");
            }

            // Actualizar estado de la venta
            venta.setAnulada(true);
            venta.setFechaAnulacion(LocalDateTime.now());
            venta.setMotivoAnulacion(motivo);
            
            Usuario usuario = usuarioServicio.obtenerPorId(usuarioId);
            if (usuario == null) {
                throw new RuntimeException("Usuario no encontrado");
            }
            venta.setUsuarioAnulacion(usuario);

            // Devolver productos al inventario
            for (DetalleVenta detalle : venta.getDetalles()) {
                Producto producto = detalle.getProducto();
                int cantidadDevuelta = detalle.getCantidad();
                producto.setCantidad(producto.getCantidad() + cantidadDevuelta);
                productoServicio.guardar(producto);
            }

            // Si es una venta a crédito, actualizar el crédito
            if (venta.getTipoVenta() == TipoVenta.CREDITO && venta.getCredito() != null) {
                // Actualizar el crédito disponible del cliente
                Cliente cliente = venta.getCliente();
                if (cliente != null) {
                    Double creditoDisponible = cliente.getCreditoDisponible();
                    if (creditoDisponible == null) {
                        creditoDisponible = 0.0;
                    }
                    clienteServicio.actualizarCreditoDisponible(cliente.getId(), 
                        creditoDisponible + venta.getTotalVenta().doubleValue());
                }
            }

            // Guardar los cambios
            ventaRepository.save(venta);
        } catch (Exception e) {
            throw new RuntimeException("Error al anular la venta: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void eliminarVentasPorLicoreria(Long licoreriaId) {
        try {
            logger.info("Iniciando eliminación de ventas para licorería ID: {}", licoreriaId);
            List<Venta> ventas = ventaRepository.findByLicoreriaId(licoreriaId);
            
            for (Venta venta : ventas) {
                logger.debug("Procesando venta ID: {}", venta.getId());
                
                // 1. Desvincular facturas
                if (venta.getFactura() != null) {
                    logger.debug("Desvinculando factura de venta ID: {}", venta.getId());
                    venta.getFactura().setVenta(null);
                    venta.setFactura(null);
                }
                if (venta.getFacturaAnulacion() != null) {
                    logger.debug("Desvinculando factura de anulación de venta ID: {}", venta.getId());
                    venta.getFacturaAnulacion().setVentaAnulada(null);
                    venta.setFacturaAnulacion(null);
                }
                
                // 2. Desvincular créditos
                if (venta.getCredito() != null) {
                    logger.debug("Desvinculando crédito de venta ID: {}", venta.getId());
                    venta.getCredito().setVenta(null);
                    venta.setCredito(null);
                }
                
                // 3. Manejar detalles de venta y productos
                for (DetalleVenta detalle : venta.getDetalles()) {
                    Producto producto = detalle.getProducto();
                    if (producto != null) {
                        int cantidadDevuelta = detalle.getCantidad();
                        producto.setCantidad(producto.getCantidad() + cantidadDevuelta);
                        productoServicio.guardar(producto);
                        logger.debug("Producto ID: {} actualizado con cantidad devuelta: {}", 
                            producto.getId(), cantidadDevuelta);
                    }
                }
                
                // 4. Manejar cliente si es venta a crédito
                if (venta.getTipoVenta() == TipoVenta.CREDITO && venta.getCliente() != null) {
                    Cliente cliente = venta.getCliente();
                    Double creditoDisponible = cliente.getCreditoDisponible();
                    if (creditoDisponible == null) {
                        creditoDisponible = 0.0;
                    }
                    clienteServicio.actualizarCreditoDisponible(cliente.getId(), 
                        creditoDisponible + venta.getTotalVenta().doubleValue());
                    logger.debug("Crédito actualizado para cliente ID: {}", cliente.getId());
                }
                
                // 5. Limpiar referencias antes de eliminar
                venta.setCliente(null);
                venta.setLicoreria(null);
                venta.setUsuarioAnulacion(null);
                venta.getDetalles().clear();
                
                // 6. Eliminar la venta
                ventaRepository.delete(venta);
                logger.debug("Venta ID: {} eliminada", venta.getId());
            }
            
            logger.info("Eliminación de ventas completada para licorería ID: {}", licoreriaId);
        } catch (Exception e) {
            logger.error("Error al eliminar ventas de la licorería: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar ventas de la licorería: " + e.getMessage(), e);
        }
    }
}
 