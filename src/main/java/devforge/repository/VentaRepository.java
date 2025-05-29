package devforge.repository;

import devforge.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {
    List<Venta> findByLicoreriaId(Long licoreriaId);
    
    List<Venta> findByLicoreriaIdAndFechaVentaBetween(
        Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    @Query("SELECT v.metodoPago as metodo, SUM(v.totalVenta) as total " +
           "FROM Venta v " +
           "WHERE v.licoreriaId = :licoreriaId " +
           "AND v.fechaVenta BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY v.metodoPago")
    List<Map<String, Object>> obtenerVentasPorMetodoPago(
        @Param("licoreriaId") Long licoreriaId,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin);
} 