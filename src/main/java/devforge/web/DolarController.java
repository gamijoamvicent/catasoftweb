package devforge.web;

import devforge.model.PrecioDolar;
import devforge.servicio.PrecioDolarServicio;
import devforge.config.LicoreriaContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dolar/api")
public class DolarController {

    private final PrecioDolarServicio precioDolarServicio;
    private final LicoreriaContext licoreriaContext;

    public DolarController(PrecioDolarServicio precioDolarServicio, LicoreriaContext licoreriaContext) {
        this.precioDolarServicio = precioDolarServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("/tasas")
    public ResponseEntity<?> obtenerTasas() {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("Debe seleccionar una licorería primero");
            }

            Map<String, Double> tasas = new HashMap<>();
            tasas.put("bcv", precioDolarServicio.obtenerPrecioActualPorTipo(
                licoreriaContext.getLicoreriaId(), 
                PrecioDolar.TipoTasa.BCV
            ));
            tasas.put("paralelo", precioDolarServicio.obtenerPrecioActualPorTipo(
                licoreriaContext.getLicoreriaId(), 
                PrecioDolar.TipoTasa.PARALELA
            ));
            tasas.put("promedio", precioDolarServicio.obtenerPrecioActualPorTipo(
                licoreriaContext.getLicoreriaId(), 
                PrecioDolar.TipoTasa.PROMEDIO
            ));

            return ResponseEntity.ok(tasas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener las tasas: " + e.getMessage());
        }
    }
} 