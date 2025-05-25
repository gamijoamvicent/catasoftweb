package devforge.servicio;

import devforge.model.PrecioDolar;

public interface PrecioDolarServicio {
    double obtenerPrecioActual(Long licoreriaId);
    PrecioDolar obtenerUltimoPrecio(Long licoreriaId);
    void guardar(PrecioDolar precio);
}
