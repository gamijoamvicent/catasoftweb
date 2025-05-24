package devforge.repository;

import devforge.model.Licoreria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LicoreriaRepository extends JpaRepository<Licoreria, Long> {
    List<Licoreria> findByActivoTrue();
    Optional<Licoreria> findByNombre(String nombre);
    Optional<Licoreria> findByIpLocal(String ipLocal);
} 