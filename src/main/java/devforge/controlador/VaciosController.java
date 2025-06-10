package devforge.controlador;

import devforge.model.Vacio;
import devforge.servicio.VacioService;
import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Controller
@RequestMapping("/vacios")
public class VaciosController {
    
    private static final Logger logger = LoggerFactory.getLogger(VaciosController.class);
    private final VacioService vacioService;
    private final LicoreriaContext licoreriaContext;

    @Autowired
    public VaciosController(VacioService vacioService, LicoreriaContext licoreriaContext) {
        this.vacioService = vacioService;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping("")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
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
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> prestarVacios(@RequestBody Vacio vacio) {
        try {
            logger.info("Intentando registrar préstamo: {}", vacio);
            
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest().body("No hay licorería seleccionada");
            }
            
            if (vacio.getCantidad() <= 0) {
                return ResponseEntity.badRequest().body("La cantidad debe ser mayor a 0");
            }
            
            if (vacio.getValorPorUnidad() < 0) {
                return ResponseEntity.badRequest().body("El valor por unidad no puede ser negativo");
            }
            
            vacio.setLicoreria(licoreriaContext.getLicoreriaActual());
            Vacio prestamo = vacioService.registrarPrestamo(vacio);
            logger.info("Préstamo registrado exitosamente: {}", prestamo);
            return ResponseEntity.ok(prestamo);
        } catch (Exception e) {
            logger.error("Error al registrar préstamo", e);
            return ResponseEntity.badRequest().body("Error al registrar préstamo: " + e.getMessage());
        }
    }

    @PostMapping("/devolver/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> devolverVacios(@PathVariable Long id) {
        try {
            logger.info("Intentando registrar devolución para préstamo ID: {}", id);
            Vacio devolucion = vacioService.registrarDevolucion(id);
            logger.info("Devolución registrada exitosamente: {}", devolucion);
            return ResponseEntity.ok(devolucion);
        } catch (Exception e) {
            logger.error("Error al registrar devolución", e);
            return ResponseEntity.badRequest().body("Error al registrar devolución: " + e.getMessage());
        }
    }
    
    @PostMapping("/stock/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> actualizarStock(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            logger.info("Intentando actualizar stock ID: {} a cantidad: {}", id, cantidad);
            Vacio vacio = vacioService.actualizarStock(id, cantidad);
            if (vacio == null) {
                return ResponseEntity.ok(Map.of("mensaje", "Stock eliminado correctamente"));
            }
            logger.info("Stock actualizado exitosamente: {}", vacio);
            return ResponseEntity.ok(vacio);
        } catch (Exception e) {
            logger.error("Error al actualizar stock", e);
            return ResponseEntity.badRequest().body("Error al actualizar stock: " + e.getMessage());
        }
    }
    
    @PostMapping("/valor/{id}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN_LOCAL', 'SUPER_ADMIN')")
    public ResponseEntity<?> actualizarValor(@PathVariable Long id, @RequestParam double valor) {
        try {
            logger.info("Intentando actualizar valor ID: {} a valor: {}", id, valor);
            Vacio vacio = vacioService.actualizarValor(id, valor);
            logger.info("Valor actualizado exitosamente: {}", vacio);
            return ResponseEntity.ok(vacio);
        } catch (Exception e) {
            logger.error("Error al actualizar valor", e);
            return ResponseEntity.badRequest().body("Error al actualizar valor: " + e.getMessage());
        }
    }
} 