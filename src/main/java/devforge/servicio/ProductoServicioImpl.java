package devforge.servicio;

import devforge.config.LicoreriaContext;
import devforge.model.ItemVentaDto;
import devforge.model.Producto;
import devforge.repository.PrecioDolarRepository;
import devforge.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductoServicioImpl implements ProductoServicio {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PrecioDolarServicio repositorio;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Override
    public List<Producto> listarProductos() {
        if (licoreriaContext.getLicoreriaId() == null) {
            return List.of();
        }
        return productoRepository.findByLicoreriaId(licoreriaContext.getLicoreriaId());
    }

    @Override
    public double obtenerPrecioActual() {
        return repositorio.obtenerPrecioActual(); // Puedes cambiar por llamada al servicio del dólar
    }

    @Override
    public void descontarStock(Long id, int cantidad) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getCantidad() < cantidad) {
            throw new RuntimeException("Stock insuficiente");
        }

        producto.setCantidad(producto.getCantidad() - cantidad);
        productoRepository.save(producto); // ✅ Guarda el cambio en la BD
    }

    @Override
    public void guardar(Producto producto) {
        productoRepository.save(producto);
    }

    @Override
    public void eliminar(Long id) {
        var busqueda = productoRepository.findById(id).orElse(null);
        productoRepository.delete(busqueda);
    }

    @Override
    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id).orElse(null);
    }

    @Override
    public List<Producto> buscarPorNombreOCodigo(String termino) {
        if (licoreriaContext.getLicoreriaId() == null) {
            return List.of();
        }
        List<Producto> productos = productoRepository.findByLicoreriaId(licoreriaContext.getLicoreriaId());
        String lowerTermino = termino.toLowerCase();
        return productos.stream()
                .filter(p -> p.getNombre().toLowerCase().contains(lowerTermino)
                        || (p.getCodigoUnico() != null && p.getCodigoUnico().toLowerCase().contains(lowerTermino)))
                .toList();
    }

    @Override
    @Transactional
    public void descontarStockk(List<ItemVentaDto> items) {
        for (ItemVentaDto item : items) {
            Producto producto = productoRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            int nuevaCantidad = producto.getCantidad() - item.getCantidad();

            if (nuevaCantidad < 0) {
                throw new RuntimeException("Cantidad inválida para el producto: " + item.getId());
            }

            producto.setCantidad(nuevaCantidad);
            productoRepository.save(producto); // ✅ Guarda el cambio
        }
    }

    @Override
    public List<Producto> listarProductosPorLicoreria(Long licoreriaId) {
        return productoRepository.findByLicoreriaId(licoreriaId);
    }

    @Override
    public List<Producto> listarProductosPorLicoreriaYCategoria(Long licoreriaId, String categoria) {
        return productoRepository.findByLicoreriaIdAndCategoria(licoreriaId, categoria);
    }
}
