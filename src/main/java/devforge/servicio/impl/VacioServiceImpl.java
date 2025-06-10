package devforge.servicio.impl;

import devforge.model.Vacio;
import devforge.repository.VacioRepository;
import devforge.servicio.VacioService;
import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VacioServiceImpl implements VacioService {
    
    private final VacioRepository vacioRepository;
    private final LicoreriaContext licoreriaContext;
    
    @Autowired
    public VacioServiceImpl(VacioRepository vacioRepository, LicoreriaContext licoreriaContext) {
        this.vacioRepository = vacioRepository;
        this.licoreriaContext = licoreriaContext;
    }
    
    @Override
    public Vacio registrarPrestamo(Vacio vacio) {
        // Verificar si es un nuevo registro de stock
        if (vacio.getFechaPrestamo() == null) {
            // Verificar si ya existe un registro de stock
            Optional<Vacio> stockExistente = vacioRepository.findByEsStockTrueAndLicoreriaId(vacio.getLicoreria().getId());
            if (stockExistente.isPresent()) {
                throw new RuntimeException("Ya existe un registro de stock para esta licorería");
            }
            
            vacio.setEsStock(true);
            vacio.setStockDisponible(vacio.getCantidad());
            vacio.setDevuelto(true);
            return vacioRepository.save(vacio);
        }
        
        // Es un préstamo
        vacio.setEsStock(false);
        
        // Verificar stock disponible
        Vacio stock = vacioRepository.findByEsStockTrueAndLicoreriaId(vacio.getLicoreria().getId())
            .orElseThrow(() -> new RuntimeException("No hay stock registrado"));
            
        if (vacio.getCantidad() > stock.getStockDisponible()) {
            throw new RuntimeException("No hay suficiente stock disponible. Stock actual: " + stock.getStockDisponible());
        }
        
        // Actualizar stock
        stock.setStockDisponible(stock.getStockDisponible() - vacio.getCantidad());
        vacioRepository.save(stock);
        
        // Registrar préstamo
        vacio.setFechaPrestamo(LocalDateTime.now());
        vacio.setDevuelto(false);
        return vacioRepository.save(vacio);
    }
    
    @Override
    public Vacio registrarDevolucion(Long id) {
        Vacio prestamo = vacioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
            
        if (prestamo.isEsStock()) {
            throw new RuntimeException("No se puede devolver un registro de stock");
        }
        
        if (prestamo.isDevuelto()) {
            throw new RuntimeException("Este préstamo ya fue devuelto");
        }
        
        // Obtener el stock
        Vacio stock = vacioRepository.findByEsStockTrueAndLicoreriaId(prestamo.getLicoreria().getId())
            .orElseThrow(() -> new RuntimeException("No hay stock registrado"));
        
        // Devolver al stock
        stock.setStockDisponible(stock.getStockDisponible() + prestamo.getCantidad());
        vacioRepository.save(stock);
        
        // Marcar préstamo como devuelto
        prestamo.setDevuelto(true);
        prestamo.setFechaDevolucion(LocalDateTime.now());
        return vacioRepository.save(prestamo);
    }
    
    @Override
    public List<Vacio> obtenerVaciosPrestados() {
        return vacioRepository.findByEsStockFalseAndDevueltoFalseAndLicoreriaId(licoreriaContext.getLicoreriaId());
    }
    
    @Override
    public List<Vacio> obtenerVaciosDevueltos() {
        return vacioRepository.findByEsStockFalseAndDevueltoTrueAndLicoreriaId(licoreriaContext.getLicoreriaId());
    }
    
    @Override
    public Vacio obtenerStock() {
        return vacioRepository.findByEsStockTrueAndLicoreriaId(licoreriaContext.getLicoreriaId())
            .orElse(null);
    }
    
    @Override
    public Vacio actualizarStock(Long id, int nuevaCantidad) {
        Vacio vacio = vacioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vacio no encontrado"));
            
        if (!vacio.isEsStock()) {
            throw new RuntimeException("Solo se puede actualizar el stock principal");
        }
        
        // Si la cantidad es 0, eliminar el registro
        if (nuevaCantidad == 0) {
            vacioRepository.delete(vacio);
            return null;
        }
        
        // Actualizar el stock
        vacio.setStockDisponible(nuevaCantidad);
        vacio.setCantidad(nuevaCantidad);
        return vacioRepository.save(vacio);
    }
    
    @Override
    public Vacio actualizarValor(Long id, double nuevoValor) {
        Vacio vacio = vacioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vacio no encontrado"));
        vacio.setValorPorUnidad(nuevoValor);
        return vacioRepository.save(vacio);
    }
} 