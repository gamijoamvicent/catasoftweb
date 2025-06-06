package devforge.repository;

import devforge.model.ComboProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComboProductoRepository extends JpaRepository<ComboProducto, Long> {
    List<ComboProducto> findByComboId(Long comboId);
} 