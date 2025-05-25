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
    public PrecioDolar obtenerUltimoPrecio(Long licoreriaId) {
        List<PrecioDolar> precios = repositorio.findByLicoreriaIdOrderByFechaDolarDesc(licoreriaId);

        if (precios == null || precios.isEmpty()) {
            return new PrecioDolar();
        }

        return precios.get(0);
    }

    @Override
    public void guardar(PrecioDolar precio) {
        if (precio.getFechaDolar() == null) {
            precio.setFechaDolar(new Date());
        }
        repositorio.save(precio);
    }

    @Override
    public double obtenerPrecioActual(Long licoreriaId) {
        List<PrecioDolar> precios = repositorio.findByLicoreriaIdOrderByFechaDolarDesc(licoreriaId);

        if (precios == null || precios.isEmpty()) {
            return 1; // Valor por defecto
        }

        return precios.get(0).getPrecioDolar();
    }
}
