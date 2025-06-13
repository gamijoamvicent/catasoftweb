package devforge.servicio;

import devforge.model.Combo;
import devforge.repository.ComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Service
@Transactional
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepository;

    @Autowired
    public ComboServiceImpl(ComboRepository comboRepository) {
        this.comboRepository = comboRepository;
    }    @Override
    public Optional<Combo> obtenerPorId(Long id) {
        return comboRepository.findByIdAndActivoTrue(id);
    }

    @Override
    public List<Combo> listarPorLicoreria(Long licoreriaId) {
        return comboRepository.findByLicoreriaIdAndActivoTrue(licoreriaId);
    }

    @Override
    public Combo guardar(Combo combo) {
        return comboRepository.save(combo);
    }

    @Override
    public void eliminar(Long id) {
        comboRepository.deleteById(id);
    }

    @Override
    public List<Combo> buscarPorNombre(String nombre, Long licoreriaId) {
        return comboRepository.findByLicoreriaIdAndNombreContainingIgnoreCaseAndActivoTrue(licoreriaId, nombre);
    }
}
