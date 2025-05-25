package devforge.web;

import devforge.model.Cliente;
import devforge.servicio.ClienteServicio;
import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private ClienteServicio clienteServicio;

    @Autowired
    private LicoreriaContext licoreriaContext;

    @GetMapping("/buscar")
    public ResponseEntity<?> buscarClientes(@RequestParam String termino) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body("Debe seleccionar una licorería primero");
            }

            List<Cliente> clientes = clienteServicio.buscarPorNombreOApellido(
                termino, licoreriaContext.getLicoreriaId());
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Error al buscar clientes: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCliente(@PathVariable Long id) {
        try {
            if (licoreriaContext.getLicoreriaActual() == null) {
                return ResponseEntity.badRequest()
                    .body("Debe seleccionar una licorería primero");
            }

            return clienteServicio.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Error al obtener cliente: " + e.getMessage());
        }
    }
} 