package devforge.servicio.impl;

import devforge.config.LicoreriaContext;
import devforge.model.Combo;
import devforge.model.ComboProducto;
import devforge.model.Producto;
import devforge.repository.ComboRepository;
import devforge.repository.ProductoRepository;
import devforge.servicio.VentaComboServicio;
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
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("No hay items para procesar");
        }

        for (Map<String, Object> item : items) {
            Long comboId = Long.parseLong(item.get("id").toString());
            int cantidad = Integer.parseInt(item.get("cantidad").toString());
            
            Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("Combo no encontrado"));
            
            // Verificar que el combo pertenezca a la licorería actual
            if (!combo.getLicoreria().getId().equals(licoreriaContext.getLicoreriaId())) {
                throw new RuntimeException("El combo '" + combo.getNombre() + "' no pertenece a la licorería actual");
            }

            try {
                descontarStockCombo(combo, cantidad);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error al procesar el combo '" + combo.getNombre() + "': " + e.getMessage());
            }
        }
    }

    @Override
    public void descontarStockCombo(Combo combo, int cantidad) {
        for (ComboProducto comboProducto : combo.getProductos()) {
            Producto producto = comboProducto.getProducto();
            int cantidadADescontar = comboProducto.getCantidad() * cantidad;
            
            if (producto.getCantidad() < cantidadADescontar) {
                throw new RuntimeException(
                    String.format("No hay suficiente stock de %s. Stock actual: %d, Se requieren: %d",
                        producto.getNombre(),
                        producto.getCantidad(),
                        cantidadADescontar
                    )
                );
            }
            
            producto.setCantidad(producto.getCantidad() - cantidadADescontar);
            productoRepository.save(producto);
        }
    }

    @Override
    public void generarTicket(Long ventaId) {
        // Implementación pendiente
    }
} 