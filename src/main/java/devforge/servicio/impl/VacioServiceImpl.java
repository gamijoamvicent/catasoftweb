package devforge.servicio.impl;

import devforge.model.Vacio;
import devforge.repository.VacioRepository;
import devforge.servicio.VacioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VacioServiceImpl implements VacioService {
    
    private final VacioRepository vacioRepository;
    
    @Autowired
    public VacioServiceImpl(VacioRepository vacioRepository) {
        this.vacioRepository = vacioRepository;
    }
    
    @Override
    public Vacio registrarPrestamo(Vacio vacio) {
        vacio.setFechaPrestamo(LocalDateTime.now());
        vacio.setDevuelto(false);
        return vacioRepository.save(vacio);
    }
    
    @Override
    public Vacio registrarDevolucion(Long id) {
        Vacio vacio = vacioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vacio no encontrado"));
        vacio.setDevuelto(true);
        vacio.setFechaDevolucion(LocalDateTime.now());
        return vacioRepository.save(vacio);
    }
    
    @Override
    public List<Vacio> obtenerVaciosPrestados() {
        return vacioRepository.findByDevueltoFalse();
    }
    
    @Override
    public List<Vacio> obtenerVaciosDevueltos() {
        return vacioRepository.findByDevueltoTrue();
    }
    
    @Override
    public double obtenerMontoGarantiaActual() {
        return 100.0; // Monto fijo de garantía
    }
} 