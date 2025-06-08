package devforge.repositorio;

import devforge.model.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CajaRepositorio extends JpaRepository<Caja, Long> {
    List<Caja> findByLicoreriaId(Long licoreriaId);
    List<Caja> findByNombreContainingIgnoreCaseAndLicoreriaId(String nombre, Long licoreriaId);
    List<Caja> findByLicoreriaIdAndEstadoTrue(Long licoreriaId);
    List<Caja> findByProductoId(Long productoId);
}
