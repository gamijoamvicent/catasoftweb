package devforge.servicio;

import devforge.model.PrecioDolar;
import java.util.List;

public interface PrecioDolarServicio {
    double obtenerPrecioActual(Long licoreriaId);
    PrecioDolar obtenerUltimoPrecio(Long licoreriaId);
    void guardar(PrecioDolar precio);
    
    // Nuevos métodos para manejar diferentes tasas
    PrecioDolar obtenerUltimoPrecioPorTipo(Long licoreriaId, PrecioDolar.TipoTasa tipoTasa);
    List<PrecioDolar> obtenerUltimasTasas(Long licoreriaId);
    double obtenerPrecioActualPorTipo(Long licoreriaId, PrecioDolar.TipoTasa tipoTasa);
    
    // Devuelve la última tasa de cada tipo para la licorería
    default List<PrecioDolar> obtenerTasasActualesPorLicoreria(Long licoreriaId) {
        return obtenerUltimasTasas(licoreriaId);
    }
}
