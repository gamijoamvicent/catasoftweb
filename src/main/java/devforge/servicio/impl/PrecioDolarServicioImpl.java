package devforge.servicio.impl;

import devforge.model.PrecioDolar;
import devforge.model.PrecioDolar.TipoTasa;
import devforge.repositorio.PrecioDolarRepositorio;
import devforge.servicio.PrecioDolarServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class PrecioDolarServicioImpl implements PrecioDolarServicio {

    private final PrecioDolarRepositorio precioDolarRepositorio;

    @Autowired
    public PrecioDolarServicioImpl(PrecioDolarRepositorio precioDolarRepositorio) {
        this.precioDolarRepositorio = precioDolarRepositorio;
    }

    @Override
    public double obtenerPrecioActual(Long licoreriaId) {
        PrecioDolar precioDolar = obtenerUltimoPrecio(licoreriaId);
        return precioDolar != null ? precioDolar.getPrecioDolar() : 35.5;
    }

    @Override
    public PrecioDolar obtenerUltimoPrecio(Long licoreriaId) {
        List<PrecioDolar> precios = precioDolarRepositorio.findByLicoreriaIdOrderByFechaCreacionDesc(licoreriaId);
        return !precios.isEmpty() ? precios.get(0) : null;
    }

    @Override
    public void guardar(PrecioDolar precioDolar) {
        if (precioDolar.getFechaDolar() == null) {
            precioDolar.setFechaDolar(new Date());
        }
        if (precioDolar.getFechaCreacion() == null) {
            precioDolar.setFechaCreacion(LocalDateTime.now());
        }
        precioDolarRepositorio.save(precioDolar);
    }

    @Override
    public PrecioDolar obtenerUltimoPrecioPorTipo(Long licoreriaId, TipoTasa tipoTasa) {
        List<PrecioDolar> precios = precioDolarRepositorio.findByLicoreriaIdAndTipoTasaOrderByFechaCreacionDesc(
                licoreriaId, tipoTasa);
        return !precios.isEmpty() ? precios.get(0) : null;
    }

    @Override
    public List<PrecioDolar> obtenerUltimasTasas(Long licoreriaId) {
        List<PrecioDolar> todasLasTasas = precioDolarRepositorio.findByLicoreriaIdOrderByFechaCreacionDesc(licoreriaId);
        List<PrecioDolar> ultimasTasas = new ArrayList<>();

        // Obtener la última tasa para cada tipo (BCV, PROMEDIO, PARALELA)
        for (TipoTasa tipoTasa : TipoTasa.values()) {
            todasLasTasas.stream()
                    .filter(tasa -> tasa.getTipoTasa() == tipoTasa)
                    .findFirst()
                    .ifPresent(ultimasTasas::add);
        }

        return ultimasTasas;
    }

    @Override
    public double obtenerPrecioActualPorTipo(Long licoreriaId, TipoTasa tipoTasa) {
        PrecioDolar precioDolar = obtenerUltimoPrecioPorTipo(licoreriaId, tipoTasa);
        return precioDolar != null ? precioDolar.getPrecioDolar() : 35.5;
    }

    @Override
    public List<PrecioDolar> obtenerTasasActualesPorLicoreria(Long licoreriaId) {
        return obtenerUltimasTasas(licoreriaId);
    }

    @Override
    public void eliminar(Long id) {
        precioDolarRepositorio.deleteById(id);
    }

    @Override
    public Double obtenerTasaCambioActual(Long licoreriaId) {
        // Obtener la tasa más reciente (BCV por defecto)
        List<PrecioDolar> tasas = precioDolarRepositorio.findByLicoreriaIdAndTipoTasaOrderByFechaCreacionDesc(
            licoreriaId, TipoTasa.BCV);
        if (!tasas.isEmpty()) {
            return tasas.get(0).getPrecioDolar();
        }

        // Si no hay tasa BCV, obtener cualquier tasa
        tasas = precioDolarRepositorio.findByLicoreriaIdOrderByFechaCreacionDesc(licoreriaId);
        if (!tasas.isEmpty()) {
            return tasas.get(0).getPrecioDolar();
        }

        // Si no hay tasas, devolver un valor predeterminado
        return 35.5;
    }
}
