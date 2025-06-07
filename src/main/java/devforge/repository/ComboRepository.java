package devforge.repository;

import devforge.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByLicoreriaIdAndActivoTrue(Long licoreriaId);
    Optional<Combo> findByIdAndActivoTrue(Long id);
    List<Combo> findByLicoreriaIdAndNombreContainingIgnoreCaseAndActivoTrue(Long licoreriaId, String nombre);
} 