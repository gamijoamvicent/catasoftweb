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

    // Métodos para gestión básica
    List<PrecioDolar> obtenerTasasActualesPorLicoreria(Long licoreriaId);
    void eliminar(Long id);
    Double obtenerTasaCambioActual(Long licoreriaId);
}
