package devforge.servicio.impl;

import devforge.model.*;
import devforge.model.enums.TipoVenta;
import devforge.repository.VentaCajaRepository;
import devforge.repository.VentaRepository;
import devforge.repository.CajaRepository;
import devforge.repository.ProductoRepository;
import devforge.servicio.VentaCajaServicio;
import devforge.servicio.PrecioDolarServicio;
import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class VentaCajaServicioImpl implements VentaCajaServicio {

    private final VentaCajaRepository ventaCajaRepository;
    private final VentaRepository ventaRepository;
    private final CajaRepository cajaRepository;
    private final ProductoRepository productoRepository;
    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;

    @Autowired
    public VentaCajaServicioImpl(
            VentaCajaRepository ventaCajaRepository,
            VentaRepository ventaRepository,
            CajaRepository cajaRepository,
            ProductoRepository productoRepository,
            PrecioDolarServicio precioDolarServicio,
            LicoreriaContext licoreriaContext) {
        this.ventaCajaRepository = ventaCajaRepository;
        this.ventaRepository = ventaRepository;
        this.cajaRepository = cajaRepository;
        this.productoRepository = productoRepository;
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @Override
    @Transactional
    public void registrarVenta(List<Map<String, Object>> items) {
        // Crear la venta
        Venta venta = new Venta();
        venta.setLicoreria(licoreriaContext.getLicoreriaActual());
        venta.setFechaVenta(LocalDateTime.now());
        venta.setTipoVenta(TipoVenta.CONTADO);
        venta.setAnulada(false);
        venta.setTotalVenta(BigDecimal.ZERO);
        venta.setTotalVentaBs(BigDecimal.ZERO);

        // Procesar cada caja
        for (Map<String, Object> item : items) {
            Long cajaId = Long.valueOf(item.get("cajaId").toString());
            int cantidad = Integer.valueOf(item.get("cantidad").toString());
            double precio = Double.valueOf(item.get("precio").toString());
            String tipoTasa = (String) item.get("tipoTasa");

            // Obtener la caja
            Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

            // Crear el detalle de venta de caja
            VentaCaja ventaCaja = new VentaCaja();
            ventaCaja.setVenta(venta);
            ventaCaja.setCaja(caja);
            ventaCaja.setCantidad(cantidad);
            ventaCaja.setPrecioUnitario(BigDecimal.valueOf(precio));
            ventaCaja.setSubtotal(BigDecimal.valueOf(precio * cantidad));
            ventaCaja.setTipoTasa(PrecioDolar.TipoTasa.valueOf(tipoTasa));
            ventaCaja.setFechaCreacion(LocalDateTime.now());

            // Calcular subtotal en bolívares
            double tasaCambio = obtenerTasaCambio(tipoTasa);
            ventaCaja.setTasaCambioUsado(BigDecimal.valueOf(tasaCambio));
            ventaCaja.setSubtotalBolivares(
                ventaCaja.getSubtotal().multiply(BigDecimal.valueOf(tasaCambio))
            );

            // Actualizar totales de la venta
            BigDecimal totalVenta = venta.getTotalVenta() != null ? venta.getTotalVenta() : BigDecimal.ZERO;
            BigDecimal totalVentaBs = venta.getTotalVentaBs() != null ? venta.getTotalVentaBs() : BigDecimal.ZERO;
            
            venta.setTotalVenta(totalVenta.add(ventaCaja.getSubtotal()));
            venta.setTotalVentaBs(totalVentaBs.add(ventaCaja.getSubtotalBolivares()));

            // Guardar el detalle
            ventaCajaRepository.save(ventaCaja);

            // Descontar stock
            descontarStockCaja(cajaId, cantidad);
        }

        // Guardar la venta
        ventaRepository.save(venta);
    }

    @Override
    @Transactional
    public void descontarStockCaja(Long cajaId, int cantidad) {
        Caja caja = cajaRepository.findById(cajaId)
            .orElseThrow(() -> new RuntimeException("Caja no encontrada: " + cajaId));

        // Descontar stock de la caja
        caja.setStock(caja.getStock() - cantidad);
        cajaRepository.save(caja);

        // Si la caja tiene un producto asociado, descontar el stock del producto
        if (caja.getProducto() != null) {
            Producto producto = caja.getProducto();
            int unidadesADescontar = caja.getCantidadUnidades() * cantidad;
            producto.setCantidad(producto.getCantidad() - unidadesADescontar);
            productoRepository.save(producto);
        }
    }

    @Override
    public List<VentaCaja> listarVentasPorVenta(Long ventaId) {
        return ventaCajaRepository.findByVentaId(ventaId);
    }

    @Override
    public List<VentaCaja> listarVentasPorCaja(Long cajaId) {
        return ventaCajaRepository.findByCajaId(cajaId);
    }

    private double obtenerTasaCambio(String tipoTasa) {
        return precioDolarServicio.obtenerPrecioActualPorTipo(
            licoreriaContext.getLicoreriaId(),
            PrecioDolar.TipoTasa.valueOf(tipoTasa)
        );
    }
} 