package devforge.servicio;

import devforge.model.Combo;
import java.util.List;

public interface ComboServicio {
    List<Combo> listarCombosPorLicoreria(Long licoreriaId);
    Combo obtenerPorId(Long id);
    void guardar(Combo combo);
    void eliminar(Long id);
    List<Combo> buscarPorNombre(String termino);
} 