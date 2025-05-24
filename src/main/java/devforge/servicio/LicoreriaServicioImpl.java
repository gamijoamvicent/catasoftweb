package devforge.servicio;

import devforge.model.Licoreria;
import devforge.repository.LicoreriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LicoreriaServicioImpl implements LicoreriaServicio {

    private final LicoreriaRepository licoreriaRepository;

    @Autowired
    public LicoreriaServicioImpl(LicoreriaRepository licoreriaRepository) {
        this.licoreriaRepository = licoreriaRepository;
    }

    @Override
    public Licoreria guardar(Licoreria licoreria) {
        if (licoreria.getId() == null) {
            licoreria.setEstado(true);
        }
        return licoreriaRepository.save(licoreria);
    }

    @Override
    public List<Licoreria> listarTodas() {
        return licoreriaRepository.findAll();
    }

    @Override
    public List<Licoreria> listarActivas() {
        return licoreriaRepository.findByEstadoTrue();
    }

    @Override
    public Optional<Licoreria> obtenerPorId(Long id) {
        return licoreriaRepository.findById(id);
    }

    @Override
    public Optional<Licoreria> obtenerPorNombre(String nombre) {
        return licoreriaRepository.findByNombre(nombre);
    }

    @Override
    public Optional<Licoreria> obtenerPorIpLocal(String ipLocal) {
        return licoreriaRepository.findByIpLocal(ipLocal);
    }

    @Override
    public void eliminar(Long id) {
        licoreriaRepository.deleteById(id);
    }

    @Override
    public void cambiarEstado(Long id, boolean estado) {
        licoreriaRepository.findById(id).ifPresent(licoreria -> {
            licoreria.setEstado(estado);
            licoreriaRepository.save(licoreria);
        });
    }
} 