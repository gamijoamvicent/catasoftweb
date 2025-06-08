package devforge.repository;

import devforge.model.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Long> {
    List<Caja> findByLicoreriaId(Long licoreriaId);
    List<Caja> findByLicoreriaIdAndEstadoTrue(Long licoreriaId);
    List<Caja> findByProductoId(Long productoId);
    List<Caja> findByNombreContainingIgnoreCaseAndLicoreriaIdAndEstadoTrue(String nombre, Long licoreriaId);
}
