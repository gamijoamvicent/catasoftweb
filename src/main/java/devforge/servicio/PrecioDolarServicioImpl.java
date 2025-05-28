package devforge.servicio;

import devforge.model.PrecioDolar;
import devforge.repository.PrecioDolarRepository;
import java.util.ArrayList;
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
        return obtenerPrecioActualPorTipo(licoreriaId, PrecioDolar.TipoTasa.BCV);
    }

    @Override
    public PrecioDolar obtenerUltimoPrecioPorTipo(Long licoreriaId, PrecioDolar.TipoTasa tipoTasa) {
        List<PrecioDolar> precios = repositorio.findByLicoreriaIdAndTipoTasaOrderByFechaDolarDesc(licoreriaId, tipoTasa);
        if (precios == null || precios.isEmpty()) {
            PrecioDolar nuevoPrecio = new PrecioDolar();
            nuevoPrecio.setTipoTasa(tipoTasa);
            nuevoPrecio.setPrecioDolar(1.0); // Valor por defecto
            return nuevoPrecio;
        }
        return precios.get(0);
    }

    @Override
    public List<PrecioDolar> obtenerUltimasTasas(Long licoreriaId) {
        List<PrecioDolar> ultimasTasas = new ArrayList<>();
        for (PrecioDolar.TipoTasa tipo : PrecioDolar.TipoTasa.values()) {
            ultimasTasas.add(obtenerUltimoPrecioPorTipo(licoreriaId, tipo));
        }
        return ultimasTasas;
    }

    @Override
    public double obtenerPrecioActualPorTipo(Long licoreriaId, PrecioDolar.TipoTasa tipoTasa) {
        PrecioDolar ultimoPrecio = obtenerUltimoPrecioPorTipo(licoreriaId, tipoTasa);
        return ultimoPrecio.getPrecioDolar();
    }
}
