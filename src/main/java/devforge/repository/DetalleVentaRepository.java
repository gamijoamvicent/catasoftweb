package devforge.repository;

import devforge.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Modifying
    @Query("DELETE FROM DetalleVenta dv WHERE dv.venta.id = :ventaId")
    void deleteByVentaId(@Param("ventaId") Long ventaId);

    @Modifying
    @Query("DELETE FROM DetalleVenta d WHERE d.venta.licoreria.id = :licoreriaId")
    void deleteByLicoreriaId(Long licoreriaId);
} 