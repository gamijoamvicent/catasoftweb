package devforge.servicio;

import devforge.model.ConfiguracionImpresora;
import devforge.model.Licoreria;
import devforge.repository.ConfiguracionImpresoraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.util.List;

@Service
public class ConfiguracionImpresoraServicio {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionImpresoraServicio.class);

    @Autowired
    private ConfiguracionImpresoraRepository repository;

    public ConfiguracionImpresora obtenerPorLicoreria(Long licoreriaId) {
        return repository.findByLicoreriaId(licoreriaId)
                .orElse(new ConfiguracionImpresora());
    }

    public ConfiguracionImpresora guardar(ConfiguracionImpresora configuracion) {
        logger.info("Guardando configuración de impresora para licorería: {}", configuracion.getLicoreriaId());
        return repository.save(configuracion);
    }

    public void crearConfiguracionPorDefecto(Licoreria licoreria) {
        if (repository.findByLicoreriaId(licoreria.getId()).isPresent()) {
            return;
        }

        ConfiguracionImpresora config = new ConfiguracionImpresora();
        config.setLicoreria(licoreria);
        config.setDeteccionAutomatica(true);
        config.setVelocidadBaudios(9600);
        config.setBitsDatos(8);
        config.setBitsParada(1);
        config.setParidad("NONE");
        config.setAnchoPapel(80);
        config.setDpi(203);
        config.setCorteAutomatico(true);
        config.setActiva(false);

        repository.save(config);
        logger.info("Creada configuración por defecto para licorería: {}", licoreria.getNombre());
    }

    public boolean probarImpresion(ConfiguracionImpresora config) {
        try {
            logger.info("Realizando prueba de impresión en puerto: {}", config.getPuertoCom());
            // Aquí iría la lógica de conexión con la impresora y prueba de impresión
            // Implementación simplificada para fines de demostración
            return true;
        } catch (Exception e) {
            logger.error("Error al realizar prueba de impresión: {}", e.getMessage());
            return false;
        }
    }

    public boolean probarImpresionPorSistema(ConfiguracionImpresora config, String sistema) {
        try {
            logger.info("Prueba de impresión para sistema: {}", sistema);
            String contenido = config.getTicketTexto() != null && !config.getTicketTexto().isEmpty()
                    ? config.getTicketTexto()
                    : "Prueba de impresión";
            if ("linux".equalsIgnoreCase(sistema)) {
                java.nio.file.Files.write(
                    java.nio.file.Path.of("/tmp/prueba_impresion_ticket.txt"),
                    contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
                );
                logger.info("Archivo de prueba creado en /tmp/prueba_impresion_ticket.txt");
                return true;
            } else if ("windows".equalsIgnoreCase(sistema)) {
                String impresoraSeleccionada = config.getPuertoCom();
                logger.info("Impresora seleccionada para Windows: {}", impresoraSeleccionada);
                if (impresoraSeleccionada == null || impresoraSeleccionada.isBlank() || impresoraSeleccionada.equalsIgnoreCase("none")) {
                    logger.warn("No se seleccionó impresora válida para Windows");
                    throw new RuntimeException("Debe seleccionar una impresora válida (por ejemplo, Print to PDF)");
                }
                javax.print.PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
                javax.print.PrintService impresora = null;
                for (javax.print.PrintService ps : printServices) {
                    if (ps.getName().trim().equalsIgnoreCase(impresoraSeleccionada.trim())) {
                        impresora = ps;
                        break;
                    }
                }
                if (impresora == null) {
                    logger.warn("No se encontró la impresora: {}", impresoraSeleccionada);
                    throw new RuntimeException("No se encontró la impresora seleccionada: " + impresoraSeleccionada);
                }
                javax.print.DocFlavor flavor = javax.print.DocFlavor.STRING.TEXT_PLAIN;
                javax.print.Doc doc = new javax.print.SimpleDoc(contenido, flavor, null);
                javax.print.DocPrintJob job = impresora.createPrintJob();
                job.print(doc, null);
                logger.info("Enviado a imprimir en: {}", impresoraSeleccionada);
                return true;
            } else {
                logger.info("Sistema no soportado, solo se simula impresión");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error al realizar prueba de impresión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al imprimir: " + e.getMessage(), e);
        }
    }

    public List<String> detectarPuertosDisponibles() {
        try {
            // Detectar impresoras instaladas en Windows
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            List<String> impresoras = new java.util.ArrayList<>();
            for (PrintService ps : printServices) {
                impresoras.add(ps.getName());
            }
            // Agregar puertos COM manuales para compatibilidad
            impresoras.add("COM1");
            impresoras.add("COM2");
            impresoras.add("COM3");
            impresoras.add("COM4");
            return impresoras;
        } catch (Exception e) {
            logger.error("Error al detectar impresoras: {}", e.getMessage());
            // Fallback a puertos COM si hay error
            return List.of("COM1", "COM2", "COM3", "COM4");
        }
    }
}
