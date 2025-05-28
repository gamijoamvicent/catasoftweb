package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.ConfiguracionImpresora;
import devforge.model.Licoreria;
import devforge.model.Usuario;
import devforge.servicio.ConfiguracionImpresoraServicio;
import devforge.servicio.UsuarioServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/impresora")
public class ImpresoraController {
    private static final Logger logger = LoggerFactory.getLogger(ImpresoraController.class);

    @Autowired
    private LicoreriaContext licoreriaContext;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ConfiguracionImpresoraServicio configuracionServicio;

    @GetMapping("/configuracion")
    public String mostrarConfiguracion(Model model, Authentication auth) {
        // Verificar que el usuario actual tenga acceso
        Usuario usuario = usuarioServicio.obtenerPorUsername(auth.getName());
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();

        if (licoreriaActual == null) {
            return "redirect:/licorerias/seleccionar";
        }

        // Verificar permisos - solo SUPER_ADMIN y ADMIN_LOCAL pueden acceder
        if (usuario.getRol() != Usuario.Rol.SUPER_ADMIN && 
            usuario.getRol() != Usuario.Rol.ADMIN_LOCAL) {
            logger.warn("Usuario {} sin permisos intentando acceder a configuración de impresora", 
                    usuario.getUsername());
            return "redirect:/acceso-denegado";
        }

        // Si es ADMIN_LOCAL, verificar que corresponda a la licorería seleccionada
        if (usuario.getRol() == Usuario.Rol.ADMIN_LOCAL && 
            !usuario.getLicoreriaId().equals(licoreriaActual.getId())) {
            logger.warn("Admin local intentando acceder a configuración de otra licorería");
            return "redirect:/acceso-denegado";
        }

        // Obtener la configuración existente o crear una nueva
        ConfiguracionImpresora configuracion = configuracionServicio.obtenerPorLicoreria(licoreriaActual.getId());
        if (configuracion.getId() == null) {
            configuracion.setLicoreria(licoreriaActual);
        }

        model.addAttribute("configuracion", configuracion);
        model.addAttribute("licoreriaActual", licoreriaActual);
        model.addAttribute("usuario", usuario);

        return "impresora/configuracion";
    }

    @PostMapping("/guardar")
    public String guardarConfiguracion(
            @ModelAttribute ConfiguracionImpresora configuracion,
            @RequestParam(value = "ticketTexto", required = false) String ticketTexto,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = usuarioServicio.obtenerPorUsername(auth.getName());
        Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();

        // Verificar permisos
        if ((usuario.getRol() != Usuario.Rol.SUPER_ADMIN && 
             usuario.getRol() != Usuario.Rol.ADMIN_LOCAL) ||
            (usuario.getRol() == Usuario.Rol.ADMIN_LOCAL && 
             !usuario.getLicoreriaId().equals(licoreriaActual.getId()))) {
            return "redirect:/acceso-denegado";
        }

        // Asegurar que la configuración corresponde a la licorería actual
        configuracion.setLicoreria(licoreriaActual);

        // Si detección automática está activada, borrar el puerto manual
        if (Boolean.TRUE.equals(configuracion.getDeteccionAutomatica())) {
            configuracion.setPuertoCom(null);
        }

        // Guardar el texto del ticket
        configuracion.setTicketTexto(ticketTexto);

        configuracionServicio.guardar(configuracion);

        redirectAttributes.addFlashAttribute("mensaje", "Configuración guardada correctamente");
        redirectAttributes.addFlashAttribute("clase", "success");

        return "redirect:/impresora/configuracion";
    }

    @GetMapping("/detectar-puertos")
    @ResponseBody
    public ResponseEntity<List<String>> detectarPuertos() {
        List<String> puertos = configuracionServicio.detectarPuertosDisponibles();
        return ResponseEntity.ok(puertos);
    }

    @PostMapping("/probar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> probarImpresion(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        logger.info("[DEBUG] Iniciando /impresora/probar con payload: {}", payload);
        try {
            Usuario usuario = usuarioServicio.obtenerPorUsername(auth.getName());
            logger.info("[DEBUG] Usuario autenticado: {}", usuario.getUsername());

            // Verificar permisos
            if (usuario.getRol() != Usuario.Rol.SUPER_ADMIN && 
                usuario.getRol() != Usuario.Rol.ADMIN_LOCAL) {
                logger.warn("[DEBUG] Usuario sin permisos");
                return ResponseEntity.status(403).body(Map.of(
                    "exito", false,
                    "mensaje", "No tiene permisos para realizar esta acción"
                ));
            }

            // Convertir el payload a ConfiguracionImpresora
            ConfiguracionImpresora config = new ConfiguracionImpresora();
            config.setPuertoCom((String) payload.get("puertoCom"));
            config.setDeteccionAutomatica(Boolean.valueOf(String.valueOf(payload.get("deteccionAutomatica"))));
            config.setVelocidadBaudios(payload.get("velocidadBaudios") != null ? Integer.valueOf(String.valueOf(payload.get("velocidadBaudios"))) : 9600);
            config.setBitsDatos(payload.get("bitsDatos") != null ? Integer.valueOf(String.valueOf(payload.get("bitsDatos"))) : 8);
            config.setBitsParada(payload.get("bitsParada") != null ? Integer.valueOf(String.valueOf(payload.get("bitsParada"))) : 1);
            config.setParidad((String) payload.getOrDefault("paridad", "NONE"));
            config.setAnchoPapel(payload.get("anchoPapel") != null ? Integer.valueOf(String.valueOf(payload.get("anchoPapel"))) : 80);
            config.setDpi(payload.get("dpi") != null ? Integer.valueOf(String.valueOf(payload.get("dpi"))) : 203);
            config.setCorteAutomatico(Boolean.valueOf(String.valueOf(payload.get("corteAutomatico"))));
            config.setImprimirLogo(Boolean.valueOf(String.valueOf(payload.get("imprimirLogo"))));
            config.setRutaLogo((String) payload.get("rutaLogo"));
            config.setActiva(Boolean.valueOf(String.valueOf(payload.get("activa"))));
            config.setTicketTexto((String) payload.get("ticketTexto"));

            String sistema = (String) payload.get("sistema");
            logger.info("[DEBUG] Sistema recibido: {}", sistema);
            boolean resultado = configuracionServicio.probarImpresionPorSistema(config, sistema);
            logger.info("[DEBUG] Resultado de probarImpresionPorSistema: {}", resultado);
            if (resultado) {
                return ResponseEntity.ok(Map.of(
                    "exito", true,
                    "mensaje", "Prueba de impresión realizada correctamente para " + (sistema != null ? sistema : "sistema desconocido")
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "exito", false,
                    "mensaje", "Error al realizar la prueba de impresión. Verifique la configuración"
                ));
            }
        } catch (Exception e) {
            logger.error("[DEBUG] Excepción en /impresora/probar: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "exito", false,
                "mensaje", e.getMessage() != null ? e.getMessage() : "Error interno: " + e.getClass().getSimpleName()
            ));
        }
    }
}
