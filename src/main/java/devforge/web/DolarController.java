package devforge.web;

import devforge.model.TasaDolar;
import devforge.repository.TasaDolarRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DolarController {

    private final TasaDolarRepository tasaDolarRepository;

    public DolarController(TasaDolarRepository tasaDolarRepository) {
        this.tasaDolarRepository = tasaDolarRepository;
    }

    @GetMapping("/tasa-dolar")
    public ResponseEntity<?> obtenerTasaDolar() {
        try {
            TasaDolar tasaActual = tasaDolarRepository.findTopByOrderByFechaDesc()
                .orElseThrow(() -> new RuntimeException("No hay tasa de dólar registrada"));

            Map<String, Object> response = new HashMap<>();
            response.put("tasa", tasaActual.getTasa());
            response.put("fecha", tasaActual.getFecha());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener la tasa del dólar: " + e.getMessage());
        }
    }
} 