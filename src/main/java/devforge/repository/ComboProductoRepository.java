package devforge.repository;

import devforge.model.ComboProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComboProductoRepository extends JpaRepository<ComboProducto, Long> {
    List<ComboProducto> findByComboId(Long comboId);

    // Método que usa la relación entre entidades para filtrar
    @Query("SELECT cp FROM ComboProducto cp WHERE cp.combo.id = :comboId AND cp.combo.licoreria.id = :licoreriaId")
    List<ComboProducto> findByComboIdAndComboLicoreriaId(@Param("comboId") Long comboId, @Param("licoreriaId") Long licoreriaId);

    // Método directo usando el nuevo campo licoreriaId
    List<ComboProducto> findByComboIdAndLicoreriaId(Long comboId, Long licoreriaId);
} 