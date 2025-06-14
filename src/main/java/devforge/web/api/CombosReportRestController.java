package devforge.web.api;

import devforge.model.VentaCombo;
import devforge.servicio.VentaComboService;
import devforge.config.LicoreriaContext;
import devforge.dto.VentaComboDashboardDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CombosReportRestController {
    @Autowired
    private VentaComboService ventaComboService;
    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping("/api/reportes/combos")
    public List<VentaComboDashboardDTO> getCombosVentas() {
        var licoreria = licoreriaContext.getLicoreriaActual();
        if (licoreria == null) return List.of();
        return ventaComboService.listarPorLicoreria(licoreria.getId())
            .stream()
            .map(vc -> {
                VentaComboDashboardDTO dto = new VentaComboDashboardDTO();
                dto.id = vc.getId();
                dto.comboNombre = vc.getCombo() != null ? vc.getCombo().getNombre() : null;
                dto.valorVentaUSD = vc.getValorVentaUSD();
                dto.valorVentaBS = vc.getValorVentaBS();
                dto.tasaConversion = vc.getTasaConversion();
                dto.metodoPago = vc.getMetodoPago();
                dto.fechaVenta = vc.getFechaVenta();
                // Si tienes cliente, agrega aquí
                dto.clienteNombre = null;
                return dto;
            })
            .collect(Collectors.toList());
    }
}
