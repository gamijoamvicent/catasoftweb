package devforge.repository;

import devforge.model.PagoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface PagoCreditoRepository extends JpaRepository<PagoCredito, Long> {
    
    @Query("SELECT p FROM PagoCredito p WHERE p.credito.id = :creditoId")
    List<PagoCredito> findByCreditoId(@Param("creditoId") Long creditoId);
    
    @Query("SELECT p FROM PagoCredito p WHERE p.licoreria.id = :licoreriaId")
    List<PagoCredito> findByLicoreriaId(@Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT p FROM PagoCredito p WHERE p.licoreria.id = :licoreriaId " +
           "AND p.fechaPago BETWEEN :fechaInicio AND :fechaFin")
    List<PagoCredito> findByLicoreriaAndFechaBetween(
        @Param("licoreriaId") Long licoreriaId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin);
} 