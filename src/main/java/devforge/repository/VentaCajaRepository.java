package devforge.repository;

import devforge.model.VentaCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaCajaRepository extends JpaRepository<VentaCaja, Long> {
    List<VentaCaja> findByVentaId(Long ventaId);
    List<VentaCaja> findByVentaIdAndActivoTrue(Long ventaId);
    List<VentaCaja> findByCajaId(Long cajaId);
    List<VentaCaja> findByCajaIdAndActivoTrue(Long cajaId);
    List<VentaCaja> findByVentaIdIn(List<Long> ventaIds);
    List<VentaCaja> findByVentaIdInAndActivoTrue(List<Long> ventaIds);
    List<VentaCaja> findByFechaCreacionBetweenAndActivoTrue(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
