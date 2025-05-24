package devforge.servicio;

import devforge.model.Licoreria;
import java.util.List;
import java.util.Optional;

public interface LicoreriaServicio {
    Licoreria guardar(Licoreria licoreria);
    List<Licoreria> listarTodas();
    List<Licoreria> listarActivas();
    Optional<Licoreria> obtenerPorId(Long id);
    Optional<Licoreria> obtenerPorNombre(String nombre);
    Optional<Licoreria> obtenerPorIpLocal(String ipLocal);
    void eliminar(Long id);
    void cambiarEstado(Long id, boolean estado);
} 