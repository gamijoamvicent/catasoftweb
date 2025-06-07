package devforge.servicio;

import devforge.config.LicoreriaContext;
import devforge.model.Combo;
import devforge.repository.ComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ComboServicioImpl implements ComboServicio {

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Override
    @Transactional(readOnly = true)
    public List<Combo> listarCombosPorLicoreria(Long licoreriaId) {
        List<Combo> combos = comboRepository.findByLicoreriaIdAndActivoTrue(licoreriaId);
        // Inicializar la colección de productos para cada combo
        combos.forEach(combo -> combo.getProductos().size());
        return combos;
    }

    @Override
    @Transactional(readOnly = true)
    public Combo obtenerPorId(Long id) {
        Combo combo = comboRepository.findByIdAndActivoTrue(id).orElse(null);
        if (combo != null) {
            // Inicializar la colección de productos
            combo.getProductos().size();
        }
        return combo;
    }

    @Override
    public void guardar(Combo combo) {
        comboRepository.save(combo);
    }

    @Override
    public void eliminar(Long id) {
        if (licoreriaContext.getLicoreriaId() == null) {
            throw new RuntimeException("Debes seleccionar una licorería antes de eliminar combos.");
        }
        var combo = comboRepository.findById(id).orElse(null);
        if (combo != null) {
            combo.setActivo(false);
            comboRepository.save(combo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Combo> buscarPorNombre(String termino) {
        if (licoreriaContext.getLicoreriaId() == null) {
            return List.of();
        }
        List<Combo> combos = comboRepository.findByLicoreriaIdAndNombreContainingIgnoreCaseAndActivoTrue(
            licoreriaContext.getLicoreriaId(), 
            termino
        );
        // Inicializar la colección de productos para cada combo
        combos.forEach(combo -> combo.getProductos().size());
        return combos;
    }
} 