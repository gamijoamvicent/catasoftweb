package devforge.repositorio;

import devforge.model.PrecioDolar;
import devforge.model.PrecioDolar.TipoTasa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecioDolarRepositorio extends JpaRepository<PrecioDolar, Long> {
    List<PrecioDolar> findByLicoreriaIdOrderByFechaCreacionDesc(Long licoreriaId);
    List<PrecioDolar> findByLicoreriaIdAndTipoTasaOrderByFechaCreacionDesc(Long licoreriaId, TipoTasa tipoTasa);
}
