package devforge.servicio;

import devforge.model.PrecioDolar;
import devforge.repository.PrecioDolarRepository;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PrecioDolarServicioImpl implements PrecioDolarServicio {

    @Autowired
    private PrecioDolarRepository repositorio;

    @Override
    public PrecioDolar obtenerUltimoPrecio() {
        List<PrecioDolar> precios = repositorio.findAll();

        if (precios == null || precios.isEmpty()) {
            return new PrecioDolar();
        }

        // Ordena la lista por fecha descendente
        return precios.stream()
                .max(Comparator.comparing(PrecioDolar::getFechaDolar))
                .orElse(new PrecioDolar());
    }

    @Override
    public void guardar(PrecioDolar precio) {
        if (precio.getFechaDolar() == null) {
            precio.setFechaDolar(new Date()); // Asignamos la fecha actual si no viene
        }
        repositorio.save(precio);
    }

    @Override
    public double obtenerPrecioActual() {
        List<PrecioDolar> precios = repositorio.findAll();

        if (precios == null || precios.isEmpty()) {
            return 1; // Valor por defecto
        }

        // Devuelve el último precio guardado
        return precios.get(precios.size() - 1).getPrecioDolar();
    }
}
