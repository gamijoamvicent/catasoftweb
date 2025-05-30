package devforge.servicio;

import devforge.model.Licoreria;
import devforge.repository.LicoreriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LicoreriaServicioImpl implements LicoreriaServicio {
    private static final Logger logger = LoggerFactory.getLogger(LicoreriaServicioImpl.class);

    private final LicoreriaRepository licoreriaRepository;

    @Autowired
    public LicoreriaServicioImpl(LicoreriaRepository licoreriaRepository) {
        this.licoreriaRepository = licoreriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Licoreria> listarTodas() {
        logger.debug("Listando todas las licorerías");
        return licoreriaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Licoreria> listarActivas() {
        logger.debug("Listando licorerías activas");
        return licoreriaRepository.findByEstadoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Licoreria> obtenerPorId(Long id) {
        logger.debug("Buscando licorería por ID: {}", id);
        return licoreriaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Licoreria> obtenerPorNombre(String nombre) {
        logger.debug("Buscando licorería por nombre: {}", nombre);
        return licoreriaRepository.findByNombre(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Licoreria> obtenerPorIpLocal(String ipLocal) {
        logger.debug("Buscando licorería por IP local: {}", ipLocal);
        return licoreriaRepository.findByIpLocal(ipLocal);
    }

    @Override
    @Transactional
    public Licoreria guardar(Licoreria licoreria) {
        logger.debug("Guardando licorería: {}", licoreria.getNombre());
        if (licoreria.getId() == null) {
            licoreria.setEstado(true);
        }
        return licoreriaRepository.save(licoreria);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        logger.debug("Eliminando licorería ID: {}", id);
        licoreriaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, boolean estado) {
        logger.debug("Cambiando estado de licorería ID: {} a {}", id, estado);
        licoreriaRepository.findById(id).ifPresent(licoreria -> {
            licoreria.setEstado(estado);
            licoreriaRepository.save(licoreria);
        });
    }
} 