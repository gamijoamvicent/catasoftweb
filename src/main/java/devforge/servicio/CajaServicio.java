package devforge.servicio;

import devforge.model.Caja;
import java.util.List;
import java.util.Optional;

public interface CajaServicio {
    Caja guardar(Caja caja);

    List<Caja> listarTodos();

    List<Caja> listarPorLicoreria(Long licoreriaId);

    List<Caja> listarCajasPorLicoreria(Long licoreriaId);

    List<Caja> listarActivosPorLicoreria(Long licoreriaId);

    List<Caja> buscarPorNombre(String nombre, Long licoreriaId);

    List<Caja> buscarCajasPorNombreYLicoreria(String termino, Long licoreriaId);

    List<Caja> buscarPorProducto(Long productoId);

    Optional<Caja> buscarPorId(Long id);

    void eliminar(Long id);

    void actualizarStock(Long id, int cantidad);

    void cambiarEstado(Long id, boolean nuevoEstado);
}
