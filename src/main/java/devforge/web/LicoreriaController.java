package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.LicoreriaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/licorerias")
public class LicoreriaController {

    @Autowired
    private LicoreriaServicio licoreriaServicio;

    @Autowired
    private LicoreriaContext licoreriaContext;

    // Vista de selección de licorería
    @GetMapping("/seleccionar")
    public String mostrarSeleccionLicoreria(Model model, Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        
        if (usuario.getRol() == Usuario.Rol.SUPER_ADMIN) {
            model.addAttribute("licorerias", licoreriaServicio.listarTodas());
        } else {
            model.addAttribute("licorerias", licoreriaServicio.listarActivas());
        }
        
        return "licorerias/seleccionar";
    }

    // Procesar selección de licorería
    @PostMapping("/seleccionar")
    public String seleccionarLicoreria(@RequestParam Long licoreriaId, RedirectAttributes redirectAttrs) {
        licoreriaServicio.obtenerPorId(licoreriaId).ifPresent(licoreria -> {
            licoreriaContext.setLicoreriaActual(licoreria);
        });
        return "redirect:/";
    }

    // Vista de gestión de licorerías (solo SUPER_ADMIN)
    @GetMapping("/gestionar")
    public String gestionarLicorerias(Model model) {
        model.addAttribute("licorerias", licoreriaServicio.listarTodas());
        model.addAttribute("nuevaLicoreria", new Licoreria());
        return "licorerias/gestionar";
    }

    // Guardar nueva licorería
    @PostMapping("/guardar")
    public String guardarLicoreria(@ModelAttribute Licoreria licoreria, RedirectAttributes redirectAttrs) {
        try {
            licoreriaServicio.guardar(licoreria);
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Licorería guardada exitosamente");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al guardar la licorería");
        }
        return "redirect:/licorerias/gestionar";
    }

    // Cambiar estado de licorería
    @PostMapping("/{id}/cambiar-estado")
    @ResponseBody
    public String cambiarEstado(@PathVariable Long id, @RequestParam boolean activar) {
        try {
            if (activar) {
                licoreriaServicio.activar(id);
            } else {
                licoreriaServicio.desactivar(id);
            }
            return "OK";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    // Eliminar licorería
    @PostMapping("/{id}/eliminar")
    @ResponseBody
    public String eliminarLicoreria(@PathVariable Long id) {
        try {
            licoreriaServicio.eliminar(id);
            return "OK";
        } catch (Exception e) {
            return "ERROR";
        }
    }
} 