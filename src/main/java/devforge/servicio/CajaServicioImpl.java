package devforge.servicio;

import devforge.model.Caja;
import devforge.repository.CajaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CajaServicioImpl implements CajaServicio {

    @Autowired
    private CajaRepository cajaRepository;

    @Override
    public Caja guardar(Caja caja) {
        return cajaRepository.save(caja);
    }

    @Override
    public List<Caja> listarTodos() {
        return cajaRepository.findAll();
    }

    @Override
    public List<Caja> listarPorLicoreria(Long licoreriaId) {
        return cajaRepository.findByLicoreriaId(licoreriaId);
    }

    @Override
    public List<Caja> listarActivosPorLicoreria(Long licoreriaId) {
        return cajaRepository.findByLicoreriaIdAndEstadoTrue(licoreriaId);
    }

    @Override
    public List<Caja> buscarPorNombre(String nombre, Long licoreriaId) {
        return cajaRepository.findByNombreContainingIgnoreCaseAndLicoreriaIdAndEstadoTrue(nombre, licoreriaId);
    }

    @Override
    public List<Caja> buscarPorProducto(Long productoId) {
        return cajaRepository.findByProductoId(productoId);
    }

    @Override
    public Optional<Caja> buscarPorId(Long id) {
        return cajaRepository.findById(id);
    }

    @Override
    public void eliminar(Long id) {
        cajaRepository.deleteById(id);
    }

    @Override
    public void cambiarEstado(Long id, boolean nuevoEstado) {
        cajaRepository.findById(id).ifPresent(caja -> {
            caja.setEstado(nuevoEstado);
            cajaRepository.save(caja);
        });
    }
}
