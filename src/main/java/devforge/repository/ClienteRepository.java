package devforge.repository;

import devforge.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    @Query("SELECT c FROM Cliente c WHERE c.licoreria.id = :licoreriaId")
    List<Cliente> findByLicoreria(@Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT c FROM Cliente c WHERE c.cedula = :cedula AND c.licoreria.id = :licoreriaId")
    Optional<Cliente> findByCedulaAndLicoreria(@Param("cedula") String cedula, @Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT c FROM Cliente c WHERE (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(c.apellido) LIKE LOWER(CONCAT('%', :termino, '%'))) AND c.licoreria.id = :licoreriaId")
    List<Cliente> findByNombreOrApellidoContaining(@Param("termino") String termino, @Param("licoreriaId") Long licoreriaId);
    
    @Query("SELECT c FROM Cliente c WHERE c.estado = true AND c.licoreria.id = :licoreriaId")
    List<Cliente> findByEstadoTrueAndLicoreria(@Param("licoreriaId") Long licoreriaId);

    @Query("SELECT c FROM Cliente c WHERE c.licoreria.id = :licoreriaId")
    List<Cliente> findByLicoreriaId(@Param("licoreriaId") Long licoreriaId);
} 