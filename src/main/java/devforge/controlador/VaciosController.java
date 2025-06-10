package devforge.controlador;

import devforge.model.Vacio;
import devforge.servicio.VacioService;
import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/vacios")
public class VaciosController {

    private final VacioService vacioService;
    private final LicoreriaContext licoreriaContext;

    @Autowired
    public VaciosController(VacioService vacioService, LicoreriaContext licoreriaContext) {
        this.vacioService = vacioService;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("")
    public String mostrarVacios(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        
        model.addAttribute("stock", vacioService.obtenerStock());
        model.addAttribute("vaciosPrestados", vacioService.obtenerVaciosPrestados());
        model.addAttribute("vaciosDevueltos", vacioService.obtenerVaciosDevueltos());
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "vacios";
    }

    @PostMapping("/prestar")
    @ResponseBody
    public ResponseEntity<?> prestarVacios(@RequestBody Vacio vacio) {
        try {
            if (vacio.getCantidad() <= 0) {
                return ResponseEntity.badRequest().body("La cantidad debe ser mayor a 0");
            }
            
            if (vacio.getValorPorUnidad() < 0) {
                return ResponseEntity.badRequest().body("El valor por unidad no puede ser negativo");
            }
            
            vacio.setLicoreria(licoreriaContext.getLicoreriaActual());
            Vacio prestamo = vacioService.registrarPrestamo(vacio);
            return ResponseEntity.ok(prestamo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar préstamo: " + e.getMessage());
        }
    }

    @PostMapping("/devolver/{id}")
    @ResponseBody
    public ResponseEntity<?> devolverVacios(@PathVariable Long id) {
        try {
            Vacio devolucion = vacioService.registrarDevolucion(id);
            return ResponseEntity.ok(devolucion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al registrar devolución: " + e.getMessage());
        }
    }
    
    @PostMapping("/stock/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarStock(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            Vacio vacio = vacioService.actualizarStock(id, cantidad);
            if (vacio == null) {
                return ResponseEntity.ok(Map.of("mensaje", "Stock eliminado correctamente"));
            }
            return ResponseEntity.ok(vacio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar stock: " + e.getMessage());
        }
    }
    
    @PostMapping("/valor/{id}")
    @ResponseBody
    public ResponseEntity<?> actualizarValor(@PathVariable Long id, @RequestParam double valor) {
        try {
            Vacio vacio = vacioService.actualizarValor(id, valor);
            return ResponseEntity.ok(vacio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al actualizar valor: " + e.getMessage());
        }
    }
} 