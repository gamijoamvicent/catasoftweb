package devforge.web;

import devforge.model.*;
import devforge.model.enums.MetodoPago;
import devforge.model.enums.TipoVenta;
import devforge.servicio.*;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.RoundingMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/ventas")
public class NuevaVentaController {

    private final ProductoServicio productoServicio;
    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;
    private final VentaServicio ventaServicio;
    private final ClienteServicio clienteServicio;
    private final CreditoServicio creditoServicio;
    private final ConfiguracionImpresoraServicio impresoraConfigServicio;
    private final PdfServicio pdfServicio;

    public NuevaVentaController(
            ProductoServicio productoServicio,
            PrecioDolarServicio precioDolarServicio,
            LicoreriaContext licoreriaContext,
            VentaServicio ventaServicio,
            ClienteServicio clienteServicio,
            CreditoServicio creditoServicio,
            ConfiguracionImpresoraServicio impresoraConfigServicio,
            PdfServicio pdfServicio) {
        this.productoServicio = productoServicio;
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
        this.ventaServicio = ventaServicio;
        this.clienteServicio = clienteServicio;
        this.creditoServicio = creditoServicio;
        this.impresoraConfigServicio = impresoraConfigServicio;
        this.pdfServicio = pdfServicio;
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevaVenta(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority())
                .orElse("Sin rol");

        // Obtener todas las tasas actuales
        List<PrecioDolar> tasasActuales = precioDolarServicio.obtenerUltimasTasas(licoreriaContext.getLicoreriaId());
        List<Producto> productos = productoServicio.listarProductosPorLicoreria(licoreriaContext.getLicoreriaId());
        
        model.addAttribute("tasasActuales", tasasActuales);
        model.addAttribute("productos", productos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        model.addAttribute("usuarioActivo", username);
        model.addAttribute("rolUsuario", rol);
        
        return "ventas/nuevaVenta";
    }

    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmarVenta(@RequestBody Map<String, Object> payload) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe seleccionar una licorería primero"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) payload.get("items");
            String metodoPagoStr = (String) payload.get("metodoPago");
            String tipoVentaStr = (String) payload.get("tipoVenta");
            Long clienteId = payload.get("clienteId") != null ? 
                Long.valueOf(payload.get("clienteId").toString()) : null;

            // Validaciones básicas
            if (itemsMap == null || itemsMap.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Debe agregar al menos un producto"));
            }

            if (metodoPagoStr == null || tipoVentaStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos de venta incompletos"));
            }

            TipoVenta tipoVenta = TipoVenta.valueOf(tipoVentaStr);
            
            // Validación de cliente para venta a crédito
            Cliente cliente = null;
            if (tipoVenta == TipoVenta.CREDITO) {
                if (clienteId == null) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe seleccionar un cliente para ventas a crédito"));
                }
                
                cliente = clienteServicio.buscarPorId(clienteId)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
                
                if (!cliente.isEstado()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "El cliente está inactivo"));
                }

                if (!cliente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "El cliente no pertenece a esta licorería"));
                }
            }

            // Procesar items y validar stock
            List<ItemVentaDto> items = new ArrayList<>();
            List<Producto> productos = new ArrayList<>();
            BigDecimal totalVenta = BigDecimal.ZERO;

            for (Map<String, Object> item : itemsMap) {
                Long productoId = Long.valueOf(item.get("id").toString());
                Integer cantidad = Integer.valueOf(item.get("cantidad").toString());

                Producto producto = productoServicio.obtenerPorId(productoId);
                if (producto == null) {
                    throw new RuntimeException("Producto no encontrado: " + productoId);
                }

                if (!producto.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                    throw new RuntimeException("El producto no pertenece a esta licorería");
                }

                if (producto.getCantidad() < cantidad) {
                    throw new RuntimeException(
                        String.format("Stock insuficiente para %s. Disponible: %d, Solicitado: %d",
                            producto.getNombre(), producto.getCantidad(), cantidad));
                }

                ItemVentaDto itemDto = new ItemVentaDto(productoId, cantidad);
                items.add(itemDto);
                productos.add(producto);

                BigDecimal subtotal = BigDecimal.valueOf(producto.getPrecioVenta())
                    .multiply(BigDecimal.valueOf(cantidad));
                totalVenta = totalVenta.add(subtotal);
            }

            // Validar límite de crédito si es venta a crédito
            if (tipoVenta == TipoVenta.CREDITO) {
                if (totalVenta.doubleValue() > cliente.getCreditoDisponible()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", 
                            String.format("El monto de la venta ($%.2f) excede el crédito disponible ($%.2f)",
                                totalVenta.doubleValue(), cliente.getCreditoDisponible())));
                }
            }

            // Crear la venta
            Venta venta = new Venta();
            List<DetalleVenta> detalles = new ArrayList<>();
            
            venta.setLicoreria(licoreriaContext.getLicoreriaActual());
            venta.setTotalVenta(totalVenta);
            venta.setMetodoPago(MetodoPago.valueOf(metodoPagoStr));
            venta.setTipoVenta(tipoVenta);
            venta.setCliente(cliente);
            venta.setFechaVenta(LocalDateTime.now());

            // Crear los detalles
            for (int i = 0; i < items.size(); i++) {
                ItemVentaDto item = items.get(i);
                Producto producto = productos.get(i);
                
                DetalleVenta detalle = new DetalleVenta();
                detalle.setVenta(venta);
                detalle.setProducto(producto);
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(BigDecimal.valueOf(producto.getPrecioVenta()));
                detalle.setSubtotal(BigDecimal.valueOf(producto.getPrecioVenta())
                    .multiply(BigDecimal.valueOf(item.getCantidad())));
                detalle.setFechaCreacion(LocalDateTime.now());
                
                // Obtener la tasa de cambio actual para la licorería y tipo de tasa del producto
                PrecioDolar.TipoTasa tipoTasa = producto.getTipoTasa() != null ?
                    PrecioDolar.TipoTasa.valueOf(producto.getTipoTasa().toUpperCase()) : PrecioDolar.TipoTasa.BCV;
                PrecioDolar tasa = precioDolarServicio.obtenerUltimoPrecioPorTipo(licoreriaContext.getLicoreriaId(), tipoTasa);
                detalle.setTasaCambioUsado(BigDecimal.valueOf(tasa.getPrecioDolar()));
                detalle.setTipoTasaUsado(tipoTasa);

                detalles.add(detalle);
            }
            venta.setDetalles(detalles);

            // Guardar la venta
            ventaServicio.guardar(venta);

            // Si es venta a crédito, crear el crédito
            if (tipoVenta == TipoVenta.CREDITO) {
                creditoServicio.crearCredito(venta);
            }

            // Descontar el stock
            for (ItemVentaDto item : items) {
                productoServicio.descontarStock(item.getId(), item.getCantidad());
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "✅ Venta registrada exitosamente",
                "ventaId", venta.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "❌ Error al procesar la venta: " + e.getMessage()));
        }
    }

    @GetMapping("/productos")
    @ResponseBody
    public ResponseEntity<?> obtenerProductos() {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Debes seleccionar una licorería primero"));
            }
            List<Producto> productos = productoServicio.listarProductos().stream()
                .filter(p -> p.getLicoreriaId().equals(licoreriaContext.getLicoreriaId()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al obtener productos: " + e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<?> buscarProductos(@RequestParam String termino) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.status(400)
                    .body(Map.of("error", "Debes seleccionar una licorería primero"));
            }
            List<Producto> productos = productoServicio.buscarPorNombreOCodigo(termino).stream()
                .filter(p -> p.getLicoreriaId().equals(licoreriaContext.getLicoreriaId()))
                .collect(Collectors.toList());
            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Error al buscar productos: " + e.getMessage()));
        }
    }

    @GetMapping("/detalle/{id}")
    public String verDetalleVenta(@PathVariable Long id, Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        Venta venta = ventaServicio.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (!venta.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
            throw new RuntimeException("No tienes permiso para ver esta venta");
        }

        model.addAttribute("venta", venta);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());

        return "ventas/detalle";
    }

    @GetMapping("/ticket/{ventaId}")
    @ResponseBody
    public ResponseEntity<?> obtenerTicketVenta(@PathVariable Long ventaId) {
        try {
            Venta venta = ventaServicio.buscarPorId(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
            Licoreria licoreria = licoreriaContext.getLicoreriaActual();
            if (licoreria == null || !venta.getLicoreriaId().equals(licoreria.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "No tienes permiso para ver esta venta"));
            }
            // Obtener configuración de impresora para la licorería
            ConfiguracionImpresora config = null;
            if (licoreria != null) {
                config = impresoraConfigServicio.obtenerPorLicoreria(licoreria.getId());
            }
            String impresora = (config != null && config.getPuertoCom() != null && !config.getPuertoCom().isBlank())
                ? config.getPuertoCom() : null;
            // Formato editable o por defecto
            String formato = (config != null && config.getTicketTexto() != null && !config.getTicketTexto().isBlank())
                ? config.getTicketTexto() : "*** {licoreria} ***\nFecha: {fecha}\n----------------------\n{detalle_productos}\n----------------------\nSUBTOTAL: {subtotal}\nTOTAL: {total}\n¡Gracias por su compra!";
            // Construir detalle de productos
            StringBuilder detalle = new StringBuilder();
            BigDecimal subtotal = BigDecimal.ZERO;
            for (DetalleVenta d : venta.getDetalles()) {
                detalle.append(d.getProducto().getNombre())
                    .append(" x")
                    .append(d.getCantidad())
                    .append("  $")
                    .append(d.getSubtotal().setScale(2, BigDecimal.ROUND_HALF_UP))
                    .append("\n");
                subtotal = subtotal.add(d.getSubtotal());
            }
            String ticket = formato
                .replace("{licoreria}", licoreria != null ? licoreria.getNombre() : "-")
                .replace("{fecha}", venta.getFechaVenta().toString())
                .replace("{detalle_productos}", detalle.toString().trim())
                .replace("{subtotal}", "$" + subtotal.setScale(2, RoundingMode.HALF_UP))
                .replace("{total}", "$" + venta.getTotalVenta().setScale(2, RoundingMode.HALF_UP));
            return ResponseEntity.ok(Map.of(
                "ticket", ticket,
                "impresora", impresora
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo generar el ticket: " + e.getMessage()));
        }
    }

    @GetMapping("/venta/{id}/ticket-pdf")
    public ResponseEntity<byte[]> generarTicketPdf(@PathVariable Long id) {
        try {
            Venta venta = ventaServicio.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

            ConfiguracionImpresora config = impresoraConfigServicio.obtenerPorLicoreria(venta.getLicoreria().getId());
            byte[] pdfBytes = pdfServicio.generarTicketPdf(venta, config);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "ticket-venta-" + id + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static class ItemVentaDto {
        private Long id;
        private Integer cantidad;

        public ItemVentaDto(Long id, Integer cantidad) {
            this.id = id;
            this.cantidad = cantidad;
        }

        public Long getId() {
            return id;
        }

        public Integer getCantidad() {
            return cantidad;
        }
    }
}
