package devforge.repository;

import devforge.model.Licoreria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicoreriaRepository extends JpaRepository<Licoreria, Long> {
    List<Licoreria> findByEstadoTrue();
    Optional<Licoreria> findByNombre(String nombre);
    Optional<Licoreria> findByIpLocal(String ipLocal);
} 