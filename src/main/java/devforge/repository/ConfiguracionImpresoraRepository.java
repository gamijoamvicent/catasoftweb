package devforge.repository;

import devforge.model.ConfiguracionImpresora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionImpresoraRepository extends JpaRepository<ConfiguracionImpresora, Long> {
    Optional<ConfiguracionImpresora> findByLicoreriaId(Long licoreriaId);
}
