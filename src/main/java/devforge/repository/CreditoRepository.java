package devforge.repository;

import devforge.model.Credito;
import devforge.model.Credito.EstadoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CreditoRepository extends JpaRepository<Credito, Long> {
    
    @Query("SELECT c FROM Credito c WHERE c.cliente.id = :clienteId AND c.licoreria.id = :licoreriaId")
    List<Credito> findByClienteAndLicoreria(@Param("clienteId") Long clienteId, @Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT c FROM Credito c WHERE c.estado = :estado AND c.licoreria.id = :licoreriaId")
    List<Credito> findByEstadoAndLicoreria(@Param("estado") EstadoCredito estado, @Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT c FROM Credito c WHERE c.fechaLimitePago < CURRENT_TIMESTAMP AND " +
           "c.estado NOT IN ('PAGADO_TOTAL') AND c.licoreria.id = :licoreriaId")
    List<Credito> findVencidosByLicoreria(@Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT c FROM Credito c WHERE c.licoreria.id = :licoreriaId")
    List<Credito> findByLicoreriaId(@Param("licoreriaId") Long licoreriaId);

    @Query("SELECT c FROM Credito c WHERE c.licoreria.id = :licoreriaId AND c.estado = :estado")
    List<Credito> findByLicoreriaIdAndEstado(@Param("licoreriaId") Long licoreriaId, @Param("estado") EstadoCredito estado);
} 