package devforge.servicio.impl;

import devforge.model.Caja;
import devforge.repositorio.CajaRepositorio;
import devforge.servicio.CajaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CajaServicioImpl implements CajaServicio {

    private final CajaRepositorio cajaRepositorio;

    @Autowired
    public CajaServicioImpl(CajaRepositorio cajaRepositorio) {
        this.cajaRepositorio = cajaRepositorio;
    }

    @Override
    @Transactional
    public Caja guardar(Caja caja) {
        return cajaRepositorio.save(caja);
    }

    @Override
    public List<Caja> listarTodos() {
        return cajaRepositorio.findAll();
    }

    @Override
    public List<Caja> listarPorLicoreria(Long licoreriaId) {
        return cajaRepositorio.findByLicoreriaId(licoreriaId);
    }

    @Override
    public List<Caja> listarCajasPorLicoreria(Long licoreriaId) {
        return cajaRepositorio.findByLicoreriaId(licoreriaId);
    }

    @Override
    public List<Caja> listarActivosPorLicoreria(Long licoreriaId) {
        return cajaRepositorio.findByLicoreriaId(licoreriaId).stream()
                .filter(caja -> caja.getEstado() != null && caja.getEstado())
                .collect(Collectors.toList());
    }

    @Override
    public List<Caja> buscarPorNombre(String nombre, Long licoreriaId) {
        return cajaRepositorio.findByNombreContainingIgnoreCaseAndLicoreriaIdAndEstadoTrue(nombre, licoreriaId);
    }

    @Override
    public List<Caja> buscarCajasPorNombreYLicoreria(String termino, Long licoreriaId) {
        return cajaRepositorio.findByNombreContainingIgnoreCaseAndLicoreriaId(termino, licoreriaId);
    }

    @Override
    public List<Caja> buscarPorProducto(Long productoId) {
        // Implementar búsqueda por producto cuando se actualice el repositorio
        return cajaRepositorio.findAll().stream()
                .filter(caja -> caja.getProductoId() != null && caja.getProductoId().equals(productoId))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Caja> buscarPorId(Long id) {
        return cajaRepositorio.findById(id);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        cajaRepositorio.deleteById(id);
    }

    @Override
    @Transactional
    public void actualizarStock(Long id, int cantidad) {
        cajaRepositorio.findById(id).ifPresent(caja -> {
            caja.setStock(caja.getStock() + cantidad);
            cajaRepositorio.save(caja);
        });
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, boolean nuevoEstado) {
        cajaRepositorio.findById(id).ifPresent(caja -> {
            caja.setEstado(nuevoEstado);
            cajaRepositorio.save(caja);
        });
    }
}
