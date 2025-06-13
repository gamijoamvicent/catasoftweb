package devforge.servicio;

import devforge.model.VentaCombo;
import devforge.repository.VentaComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VentaComboService {

    @Autowired
    private VentaComboRepository ventaComboRepository;

    public VentaCombo guardar(VentaCombo ventaCombo) {
        return ventaComboRepository.save(ventaCombo);
    }

    public List<VentaCombo> listarPorLicoreria(Long licoreriaId) {
        return ventaComboRepository.findByLicoreriaId(licoreriaId);
    }

    public List<VentaCombo> listarPorLicoreriaYFecha(
            Long licoreriaId, 
            LocalDateTime fechaInicio, 
            LocalDateTime fechaFin) {
        return ventaComboRepository.findByLicoreriaIdAndFechaVentaBetween(
            licoreriaId, fechaInicio, fechaFin);
    }

    public List<VentaCombo> listarPorComboYLicoreria(Long comboId, Long licoreriaId) {
        return ventaComboRepository.findByComboIdAndLicoreriaId(comboId, licoreriaId);
    }
}
