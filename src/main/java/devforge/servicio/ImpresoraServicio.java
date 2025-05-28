package devforge.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.print.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImpresoraServicio {
    private static final Logger logger = LoggerFactory.getLogger(ImpresoraServicio.class);

    public List<String> listarImpresorasInstaladas() {
        List<String> impresoras = new ArrayList<>();
        try {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService ps : printServices) {
                impresoras.add(ps.getName());
            }
        } catch (Exception e) {
            logger.error("Error al listar impresoras: {}", e.getMessage(), e);
        }
        return impresoras;
    }

    public boolean imprimirPrueba(String nombreImpresora, String texto) {
        try {
            logger.info("[DEBUG] Solicitud de impresión. Nombre impresora: '{}', Texto: '{}'", nombreImpresora, texto);
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            if (printServices.length == 0) {
                throw new RuntimeException("No hay impresoras instaladas en el sistema.");
            }
            PrintService impresora = null;
            for (PrintService ps : printServices) {
                logger.info("[DEBUG] Impresora encontrada: '{}'.", ps.getName());
                if (ps.getName().trim().equalsIgnoreCase(nombreImpresora.trim())) {
                    impresora = ps;
                    break;
                }
            }
            if (impresora == null) {
                throw new RuntimeException("No se encontró la impresora: " + nombreImpresora + ". Verifique el nombre exactamente como aparece en la lista.");
            }
            DocFlavor flavor = DocFlavor.STRING.TEXT_PLAIN;
            Doc doc = new SimpleDoc(texto, flavor, null);
            DocPrintJob job = impresora.createPrintJob();
            job.print(doc, null);
            logger.info("[DEBUG] Trabajo de impresión enviado correctamente a '{}'.", impresora.getName());
            return true;
        } catch (SecurityException se) {
            logger.error("[ERROR] Permiso denegado para imprimir: {}", se.getMessage(), se);
            throw new RuntimeException("Permiso denegado para imprimir. Ejecute la aplicación como administrador o revise la configuración de la impresora.");
        } catch (Exception e) {
            logger.error("[ERROR] Error al imprimir prueba: {}", e.getMessage(), e);
            throw new RuntimeException("Error al imprimir: " + e.getMessage(), e);
        }
    }
}
