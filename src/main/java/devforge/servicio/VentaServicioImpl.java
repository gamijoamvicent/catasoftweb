package devforge.servicio;

import devforge.model.Venta;
import devforge.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class VentaServicioImpl implements VentaServicio {

    @Autowired
    private VentaRepository ventaRepository;

    @Override
    public Venta guardar(Venta venta) {
        return ventaRepository.save(venta);
    }

    @Override
    public List<Venta> listarVentasPorLicoreria(Long licoreriaId) {
        return ventaRepository.findByLicoreriaIdAndFechaVentaBetween(
            licoreriaId, 
            LocalDateTime.now().minusMonths(1), 
            LocalDateTime.now()
        );
    }

    @Override
    public List<Venta> listarVentasPorLicoreriaYFecha(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return ventaRepository.findByLicoreriaIdAndFechaVentaBetween(licoreriaId, fechaInicio, fechaFin);
    }

    @Override
    public Map<String, Double> obtenerVentasPorMetodoPago(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Map<String, Object>> resultados = ventaRepository.obtenerVentasPorMetodoPago(licoreriaId, fechaInicio, fechaFin);
        
        Map<String, Double> ventasPorMetodo = new HashMap<>();
        for (Map<String, Object> resultado : resultados) {
            String metodo = ((Venta.MetodoPago) resultado.get("metodo")).name();
            Double total = ((Number) resultado.get("total")).doubleValue();
            ventasPorMetodo.put(metodo, total);
        }
        
        return ventasPorMetodo;
    }
} 