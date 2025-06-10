package devforge.repository;

import devforge.model.PrestamoVacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestamoVacioRepository extends JpaRepository<PrestamoVacio, Long> {
    List<PrestamoVacio> findByDevueltoFalseAndLicoreriaId(Long licoreriaId);
    List<PrestamoVacio> findByDevueltoTrueAndLicoreriaId(Long licoreriaId);
} 