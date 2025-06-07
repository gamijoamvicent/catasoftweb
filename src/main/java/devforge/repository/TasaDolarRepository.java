package devforge.repository;

import devforge.model.TasaDolar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TasaDolarRepository extends JpaRepository<TasaDolar, Long> {
    Optional<TasaDolar> findTopByOrderByFechaDesc();
    Optional<TasaDolar> findTopByActivaTrueOrderByFechaDesc();
} 