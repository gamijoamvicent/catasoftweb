package devforge.repository;

import devforge.model.VentaCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaComboRepository extends JpaRepository<VentaCombo, Long> {
    
    List<VentaCombo> findByLicoreriaId(Long licoreriaId);
    
    List<VentaCombo> findByLicoreriaIdAndFechaVentaBetween(
        Long licoreriaId, 
        LocalDateTime fechaInicio, 
        LocalDateTime fechaFin
    );
    
    List<VentaCombo> findByComboIdAndLicoreriaId(Long comboId, Long licoreriaId);
}
