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
        return licoreriaRepository.save(licoreria);
    }

    @Override
    public List<Licoreria> listarTodas() {
        return licoreriaRepository.findAll();
    }

    @Override
    public List<Licoreria> listarActivas() {
        return licoreriaRepository.findByActivoTrue();
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
    public void desactivar(Long id) {
        licoreriaRepository.findById(id).ifPresent(licoreria -> {
            licoreria.setActivo(false);
            licoreriaRepository.save(licoreria);
        });
    }

    @Override
    public void activar(Long id) {
        licoreriaRepository.findById(id).ifPresent(licoreria -> {
            licoreria.setActivo(true);
            licoreriaRepository.save(licoreria);
        });
    }
} 