package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.PrecioDolar;
import devforge.model.Caja;
import devforge.servicio.PrecioDolarServicio;
import devforge.servicio.CajaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/ventas/cajas")
public class VentaCajasController {

    private final LicoreriaContext licoreriaContext;
    private final PrecioDolarServicio precioDolarServicio;
    private final CajaServicio cajaServicio;

    @Autowired
    public VentaCajasController(LicoreriaContext licoreriaContext, 
                               PrecioDolarServicio precioDolarServicio,
                               CajaServicio cajaServicio) {
        this.licoreriaContext = licoreriaContext;
        this.precioDolarServicio = precioDolarServicio;
        this.cajaServicio = cajaServicio;
    }

    @GetMapping("/nueva")
    public String mostrarVentaCajas(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "ventas/venta-cajas";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<List<Caja>> buscarCajas(@RequestParam("termino") String termino) {
        Long licoreriaId = licoreriaContext.getLicoreriaId();
        if (licoreriaId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Caja> cajas = cajaServicio.buscarCajasPorNombreYLicoreria(termino, licoreriaId);
        return ResponseEntity.ok(cajas);
    }

    @GetMapping("/obtener-tasa")
    @ResponseBody
    public ResponseEntity<Double> obtenerTasaCambioActual() {
        Long licoreriaId = licoreriaContext.getLicoreriaId();
        if (licoreriaId == null) {
            return ResponseEntity.badRequest().build();
        }

        Double tasaCambio = precioDolarServicio.obtenerTasaCambioActual(licoreriaId);
        return ResponseEntity.ok(tasaCambio);
    }
}
