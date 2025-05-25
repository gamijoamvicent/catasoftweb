package devforge.repository;

import devforge.model.PagoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoCreditoRepository extends JpaRepository<PagoCredito, Long> {
    List<PagoCredito> findByCreditoId(Long creditoId);
    List<PagoCredito> findByLicoreriaId(Long licoreriaId);
    List<PagoCredito> findByLicoreriaIdAndFechaPagoBetween(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
} 