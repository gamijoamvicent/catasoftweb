package devforge.controlador;

import devforge.model.Vacio;
import devforge.servicio.VacioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vacios")
public class VaciosController {

    private final VacioService vacioService;

    @Autowired
    public VaciosController(VacioService vacioService) {
        this.vacioService = vacioService;
    }

    @GetMapping("")
    public String mostrarVacios(Model model) {
        model.addAttribute("vaciosPrestados", vacioService.obtenerVaciosPrestados());
        model.addAttribute("vaciosDevueltos", vacioService.obtenerVaciosDevueltos());
        model.addAttribute("montoGarantia", vacioService.obtenerMontoGarantiaActual());
        return "vacios";
    }

    @PostMapping("/prestar")
    @ResponseBody
    public ResponseEntity<?> prestarVacios(@RequestBody Vacio vacio) {
        try {
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
} 