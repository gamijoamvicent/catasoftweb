package devforge.servicio;

import devforge.model.Combo;
import java.util.Optional;
import java.util.List;

public interface ComboService {
    Optional<Combo> obtenerPorId(Long id);
    List<Combo> listarPorLicoreria(Long licoreriaId);
    Combo guardar(Combo combo);
    void eliminar(Long id);
    List<Combo> buscarPorNombre(String nombre, Long licoreriaId);
}
