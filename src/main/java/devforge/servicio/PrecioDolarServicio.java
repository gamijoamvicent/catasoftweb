
package devforge.servicio;

import devforge.model.PrecioDolar;

public interface PrecioDolarServicio {
    double obtenerPrecioActual();
    PrecioDolar obtenerUltimoPrecio();
    void guardar(PrecioDolar precio);
}
