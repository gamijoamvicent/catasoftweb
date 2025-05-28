package devforge.servicio;

import devforge.model.ConfiguracionImpresora;
import devforge.model.Licoreria;
import devforge.repository.ConfiguracionImpresoraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            if ("linux".equalsIgnoreCase(sistema)) {
                String contenido = config.getTicketTexto() != null && !config.getTicketTexto().isEmpty()
                        ? config.getTicketTexto()
                        : "Prueba de impresión";
                java.nio.file.Files.write(
                    java.nio.file.Path.of("/tmp/prueba_impresion_ticket.txt"),
                    contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
                );
                logger.info("Archivo de prueba creado en /tmp/prueba_impresion_ticket.txt");
                return true;
            } else {
                logger.info("Realizando prueba de impresión en puerto: {}", config.getPuertoCom());
                return true;
            }
        } catch (Exception e) {
            logger.error("Error al realizar prueba de impresión: {}", e.getMessage());
            throw new RuntimeException("Error al crear el archivo de impresión: " + e.getMessage(), e);
        }
    }

    public List<String> detectarPuertosDisponibles() {
        // Implementación simplificada - en una implementación real, esto detectaría
        // los puertos COM disponibles en el sistema
        return List.of("COM1", "COM2", "COM3", "COM4");
    }
}
