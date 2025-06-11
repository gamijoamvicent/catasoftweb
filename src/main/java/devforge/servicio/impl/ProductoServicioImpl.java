package devforge.servicio.impl;

import devforge.config.LicoreriaContext;
import devforge.model.ItemVentaDto;
import devforge.model.Producto;
import devforge.repository.ProductoRepository;
import devforge.servicio.ProductoServicio;
import devforge.servicio.PrecioDolarServicio;
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
    private PrecioDolarServicio precioDolarServicio;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Override
    public List<Producto> listarProductos() {
        if (licoreriaContext.getLicoreriaId() == null) {
            return List.of();
        }
        return productoRepository.findByLicoreriaIdAndActivoTrue(licoreriaContext.getLicoreriaId());
    }

    @Override
    public double obtenerPrecioActual() {
        if (licoreriaContext.getLicoreriaId() == null) {
            return 1.0; // Valor por defecto si no hay licorería seleccionada
        }
        return precioDolarServicio.obtenerPrecioActual(licoreriaContext.getLicoreriaId());
    }

    @Override
    public void descontarStock(Long id, int cantidad) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getCantidad() < cantidad) {
            throw new RuntimeException("Stock insuficiente");
        }

        producto.setCantidad(producto.getCantidad() - cantidad);
        productoRepository.save(producto);
    }

    @Override
    public void guardar(Producto producto) {
        productoRepository.save(producto);
    }

    @Override
    public void eliminar(Long id) {
        if (licoreriaContext == null || licoreriaContext.getLicoreriaId() == null) {
            throw new RuntimeException("Debes seleccionar una licorería antes de eliminar productos.");
        }
        var producto = productoRepository.findById(id).orElse(null);
        if (producto != null) {
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }

    @Override
    public Producto obtenerPorId(Long id) {
        return productoRepository.findByIdAndActivoTrue(id).orElse(null);
    }

    @Override
    public List<Producto> buscarPorNombreOCodigo(String termino) {
        if (licoreriaContext.getLicoreriaId() == null) {
            return List.of();
        }
        List<Producto> productos = productoRepository.findByLicoreriaIdAndActivoTrue(licoreriaContext.getLicoreriaId());
        String lowerTermino = termino.toLowerCase();
        return productos.stream()
                .filter(p -> p.getNombre().toLowerCase().contains(lowerTermino))
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
            productoRepository.save(producto);
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

    @Override
    @Transactional
    public void eliminarProductosPorLicoreria(Long licoreriaId) {
        List<Producto> productos = productoRepository.findByLicoreriaId(licoreriaId);
        for (Producto producto : productos) {
            productoRepository.delete(producto);
        }
    }

    @Override
    public List<Producto> obtenerTodos() {
        if (licoreriaContext.getLicoreriaId() == null) {
            return List.of();
        }
        return productoRepository.findByLicoreriaIdAndActivoTrue(licoreriaContext.getLicoreriaId());
    }

    @Override
    @Transactional
    public void registrarIngresoStock(Long productoId, Integer cantidad, String motivo) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!producto.isActivo()) {
            throw new RuntimeException("No se puede ingresar stock a un producto inactivo");
        }

        // Actualizar la cantidad
        producto.setCantidad(producto.getCantidad() + cantidad);
        productoRepository.save(producto);

        // Aquí podrías agregar lógica para registrar el historial de ingresos si lo necesitas
    }
} 