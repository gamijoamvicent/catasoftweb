package devforge.repository;

import devforge.model.Vacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VacioRepository extends JpaRepository<Vacio, Long> {
    List<Vacio> findByDevueltoFalse();
    List<Vacio> findByDevueltoTrue();
    Optional<Vacio> findByEsStockTrueAndLicoreriaId(Long licoreriaId);
    List<Vacio> findByEsStockFalseAndLicoreriaId(Long licoreriaId);
    List<Vacio> findByEsStockFalseAndDevueltoFalseAndLicoreriaId(Long licoreriaId);
    List<Vacio> findByEsStockFalseAndDevueltoTrueAndLicoreriaId(Long licoreriaId);
} 