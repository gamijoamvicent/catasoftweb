package devforge.servicio.impl;

import devforge.model.Vacio;
import devforge.model.PrestamoVacio;
import devforge.repository.VacioRepository;
import devforge.repository.PrestamoVacioRepository;
import devforge.servicio.VacioService;
import devforge.config.LicoreriaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class VacioServiceImpl implements VacioService {
    
    private final VacioRepository vacioRepository;
    private final PrestamoVacioRepository prestamoVacioRepository;
    private final LicoreriaContext licoreriaContext;
    
    @Autowired
    public VacioServiceImpl(VacioRepository vacioRepository, 
                          PrestamoVacioRepository prestamoVacioRepository,
                          LicoreriaContext licoreriaContext) {
        this.vacioRepository = vacioRepository;
        this.prestamoVacioRepository = prestamoVacioRepository;
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
        
        // Registrar préstamo en la tabla prestamos_vacios
        PrestamoVacio prestamo = new PrestamoVacio();
        prestamo.setCantidad(vacio.getCantidad());
        prestamo.setValorPorUnidad(vacio.getValorPorUnidad());
        prestamo.setFechaPrestamo(LocalDateTime.now());
        prestamo.setDevuelto(false);
        prestamo.setLicoreria(vacio.getLicoreria());
        prestamoVacioRepository.save(prestamo);
        
        // Convertir PrestamoVacio a Vacio para mantener compatibilidad
        Vacio vacioPrestamo = new Vacio();
        vacioPrestamo.setId(prestamo.getId());
        vacioPrestamo.setCantidad(prestamo.getCantidad());
        vacioPrestamo.setValorPorUnidad(prestamo.getValorPorUnidad());
        vacioPrestamo.setFechaPrestamo(prestamo.getFechaPrestamo());
        vacioPrestamo.setDevuelto(prestamo.isDevuelto());
        vacioPrestamo.setLicoreria(prestamo.getLicoreria());
        vacioPrestamo.setEsStock(false);
        
        return vacioPrestamo;
    }
    
    @Override
    public Vacio registrarDevolucion(Long id) {
        PrestamoVacio prestamo = prestamoVacioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));
            
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
        prestamoVacioRepository.save(prestamo);
        
        // Convertir PrestamoVacio a Vacio para mantener compatibilidad
        Vacio vacioDevolucion = new Vacio();
        vacioDevolucion.setId(prestamo.getId());
        vacioDevolucion.setCantidad(prestamo.getCantidad());
        vacioDevolucion.setValorPorUnidad(prestamo.getValorPorUnidad());
        vacioDevolucion.setFechaPrestamo(prestamo.getFechaPrestamo());
        vacioDevolucion.setFechaDevolucion(prestamo.getFechaDevolucion());
        vacioDevolucion.setDevuelto(prestamo.isDevuelto());
        vacioDevolucion.setLicoreria(prestamo.getLicoreria());
        vacioDevolucion.setEsStock(false);
        
        return vacioDevolucion;
    }
    
    @Override
    public List<Vacio> obtenerVaciosPrestados() {
        return prestamoVacioRepository.findByDevueltoFalseAndLicoreriaId(licoreriaContext.getLicoreriaId())
            .stream()
            .map(prestamo -> {
                Vacio vacio = new Vacio();
                vacio.setId(prestamo.getId());
                vacio.setCantidad(prestamo.getCantidad());
                vacio.setValorPorUnidad(prestamo.getValorPorUnidad());
                vacio.setFechaPrestamo(prestamo.getFechaPrestamo());
                vacio.setDevuelto(prestamo.isDevuelto());
                vacio.setLicoreria(prestamo.getLicoreria());
                vacio.setEsStock(false);
                return vacio;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Vacio> obtenerVaciosDevueltos() {
        return prestamoVacioRepository.findByDevueltoTrueAndLicoreriaId(licoreriaContext.getLicoreriaId())
            .stream()
            .map(prestamo -> {
                Vacio vacio = new Vacio();
                vacio.setId(prestamo.getId());
                vacio.setCantidad(prestamo.getCantidad());
                vacio.setValorPorUnidad(prestamo.getValorPorUnidad());
                vacio.setFechaPrestamo(prestamo.getFechaPrestamo());
                vacio.setFechaDevolucion(prestamo.getFechaDevolucion());
                vacio.setDevuelto(prestamo.isDevuelto());
                vacio.setLicoreria(prestamo.getLicoreria());
                vacio.setEsStock(false);
                return vacio;
            })
            .collect(Collectors.toList());
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