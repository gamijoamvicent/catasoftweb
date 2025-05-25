package devforge.web;

import devforge.model.Cliente;
import devforge.servicio.ClienteServicio;
import devforge.config.LicoreriaContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteServicio clienteServicio;
    private final LicoreriaContext licoreriaContext;

    public ClienteController(
            ClienteServicio clienteServicio,
            LicoreriaContext licoreriaContext) {
        this.clienteServicio = clienteServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping
    public String listarClientes(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        List<Cliente> clientes = clienteServicio.listarPorLicoreria(
            licoreriaContext.getLicoreriaId());

        model.addAttribute("clientes", clientes);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());

        return "clientes/listar";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        model.addAttribute("cliente", new Cliente());
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        model.addAttribute("esNuevo", true);

        return "clientes/form";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        Cliente cliente = clienteServicio.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!cliente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
            throw new RuntimeException("No tienes permiso para editar este cliente");
        }

        model.addAttribute("cliente", cliente);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        model.addAttribute("esNuevo", false);

        return "clientes/form";
    }

    @PostMapping("/guardar")
    public String guardarCliente(@ModelAttribute Cliente cliente) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        // Validar que el cliente pertenezca a la licorería actual si es una edición
        if (cliente.getId() != null) {
            Cliente clienteExistente = clienteServicio.buscarPorId(cliente.getId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

            if (!clienteExistente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
                throw new RuntimeException("No tienes permiso para editar este cliente");
            }
        }

        cliente.setLicoreria(licoreriaContext.getLicoreriaActual());
        clienteServicio.guardar(cliente);

        return "redirect:/clientes";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<List<Cliente>> buscarClientes(
            @RequestParam String termino) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Cliente> clientes;
        if (termino.matches("\\d+")) {
            // Si el término contiene solo números, buscar por cédula
            clientes = clienteServicio.buscarPorCedula(termino, licoreriaContext.getLicoreriaId())
                .map(List::of)
                .orElse(List.of());
        } else {
            // Si no, buscar por nombre o apellido
            clientes = clienteServicio.buscarPorNombreOApellido(
                termino, licoreriaContext.getLicoreriaId());
        }

        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Long id) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return ResponseEntity.badRequest().build();
        }

        Cliente cliente = clienteServicio.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!cliente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(cliente);
    }

    @PostMapping("/{id}/estado")
    @ResponseBody
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe seleccionar una licorería primero"));
        }

        Cliente cliente = clienteServicio.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!cliente.getLicoreriaId().equals(licoreriaContext.getLicoreriaId())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "No tienes permiso para modificar este cliente"));
        }

        Boolean nuevoEstado = payload.get("estado");
        if (nuevoEstado == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El estado es requerido"));
        }

        clienteServicio.cambiarEstado(id, nuevoEstado);

        return ResponseEntity.ok(Map.of(
            "mensaje", nuevoEstado ? "Cliente activado" : "Cliente desactivado",
            "estado", nuevoEstado
        ));
    }
} 