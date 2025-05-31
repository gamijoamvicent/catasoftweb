package devforge.repository;

import devforge.model.ConfiguracionImpresora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionImpresoraRepository extends JpaRepository<ConfiguracionImpresora, Long> {
    Optional<ConfiguracionImpresora> findByLicoreriaId(Long licoreriaId);

    @Modifying
    @Query("DELETE FROM ConfiguracionImpresora c WHERE c.licoreria.id = :licoreriaId")
    void deleteByLicoreriaId(Long licoreriaId);
}
