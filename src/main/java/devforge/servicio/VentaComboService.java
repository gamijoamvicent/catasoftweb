package devforge.servicio;

import devforge.dto.VentaComboDTO;
import devforge.model.Combo;
import devforge.model.ComboProducto;
import devforge.model.Licoreria;
import devforge.model.Producto;
import devforge.model.VentaCombo;
import devforge.repository.ComboRepository;
import devforge.repository.ProductoRepository;
import devforge.repository.VentaComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class VentaComboService {
    
    private final VentaComboRepository ventaComboRepository;
    private final ProductoRepository productoRepository;
    private final ComboRepository comboRepository;

    public VentaComboService(
            VentaComboRepository ventaComboRepository,
            ProductoRepository productoRepository,
            ComboRepository comboRepository) {
        this.ventaComboRepository = ventaComboRepository;
        this.productoRepository = productoRepository;
        this.comboRepository = comboRepository;
    }

    public VentaCombo guardar(VentaCombo ventaCombo) {
        return ventaComboRepository.save(ventaCombo);
    }

    public List<VentaCombo> listarPorLicoreria(Long licoreriaId) {
        return ventaComboRepository.findByLicoreriaId(licoreriaId);
    }

    public List<VentaCombo> listarPorLicoreriaYFecha(
            Long licoreriaId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        return ventaComboRepository.findByLicoreriaIdAndFechaVentaBetween(
            licoreriaId, fechaInicio, fechaFin);
    }

    public List<VentaCombo> listarPorComboYLicoreria(Long comboId, Long licoreriaId) {
        return ventaComboRepository.findByComboIdAndLicoreriaId(comboId, licoreriaId);
    }

    @Transactional
    public void decrementarStockProductos(Combo combo, int cantidadCombos) {
        List<ComboProducto> items = combo.getProductos();
        for (ComboProducto item : items) {
            // Recargar el producto desde la base de datos para tener el stock actualizado
            Producto producto = productoRepository.findById(item.getProducto().getId())
                .orElseThrow(() -> new IllegalStateException("Producto no encontrado: " + item.getProducto().getNombre()));
            int cantidadEnCombo = item.getCantidad();
            int totalADescontar = cantidadEnCombo * cantidadCombos;
            if (producto.getCantidad() < totalADescontar) {
                throw new IllegalStateException(
                    "Stock insuficiente para el producto: " + producto.getNombre()
                );
            }
            producto.setCantidad(producto.getCantidad() - totalADescontar);
            productoRepository.save(producto);
        }
    }

    @Transactional
    public void procesarCarrito(List<VentaComboDTO> ventasDTO, Licoreria licoreria) {
        // Map para acumular el total a descontar por producto
        Map<Long, Integer> productosADescontar = new HashMap<>();
        // Validar y acumular productos
        for (VentaComboDTO ventaDTO : ventasDTO) {
            if (ventaDTO.getComboId() == null || ventaDTO.getCantidad() == null || ventaDTO.getCantidad() < 1) {
                throw new IllegalArgumentException("Datos de combo/cantidad inválidos");
            }
            Combo combo = comboRepository.findById(ventaDTO.getComboId())
                .orElseThrow(() -> new IllegalArgumentException("Combo no encontrado"));
            for (ComboProducto item : combo.getProductos()) {
                Long productoId = item.getProducto().getId();
                int cantidadEnCombo = item.getCantidad();
                int total = cantidadEnCombo * ventaDTO.getCantidad();
                productosADescontar.put(productoId, productosADescontar.getOrDefault(productoId, 0) + total);
            }
        }
        // Verificar stock suficiente para todos los productos
        for (Map.Entry<Long, Integer> entry : productosADescontar.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey())
                .orElseThrow(() -> new IllegalStateException("Producto no encontrado"));
            if (producto.getCantidad() < entry.getValue()) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
            }
        }
        // Descontar stock
        for (Map.Entry<Long, Integer> entry : productosADescontar.entrySet()) {
            Producto producto = productoRepository.findById(entry.getKey()).get();
            producto.setCantidad(producto.getCantidad() - entry.getValue());
            productoRepository.save(producto);
        }
        // Registrar cada venta
        for (VentaComboDTO ventaDTO : ventasDTO) {
            Combo combo = comboRepository.findById(ventaDTO.getComboId()).get();
            VentaCombo venta = new VentaCombo();
            venta.setCombo(combo);
            venta.setValorVentaUSD(ventaDTO.getValorVentaUSD());
            venta.setValorVentaBS(ventaDTO.getValorVentaBS());
            venta.setTasaConversion(ventaDTO.getTasaConversion());
            venta.setMetodoPago(ventaDTO.getMetodoPago());
            venta.setLicoreria(licoreria);
            venta.setCantidad(ventaDTO.getCantidad());
            ventaComboRepository.save(venta);
        }
    }
}
