package devforge.servicio;

import devforge.config.LicoreriaContext;
import devforge.model.Combo;
import devforge.model.ComboProducto;
import devforge.model.Producto;
import devforge.repository.ComboRepository;
import devforge.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class VentaComboServicioImpl implements VentaComboServicio {

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Override
    public void registrarVenta(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long comboId = Long.parseLong(item.get("id").toString());
            int cantidad = Integer.parseInt(item.get("cantidad").toString());
            
            Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("Combo no encontrado"));
            
            descontarStockCombo(combo, cantidad);
        }
    }

    @Override
    public void descontarStockCombo(Combo combo, int cantidad) {
        for (ComboProducto comboProducto : combo.getProductos()) {
            Producto producto = comboProducto.getProducto();
            int cantidadADescontar = comboProducto.getCantidad() * cantidad;
            
            if (producto.getCantidad() < cantidadADescontar) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            
            producto.setCantidad(producto.getCantidad() - cantidadADescontar);
            productoRepository.save(producto);
        }
    }

    @Override
    public void generarTicket(Long ventaId) {
        // Implementar la generación del ticket
        // Por ahora está vacío
    }
} 