package devforge.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "vacios")
public class Vacio implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private int cantidad;
    private double valorPorUnidad;
    private int stockDisponible;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucion;
    private boolean devuelto;
    private boolean esStock; // Indica si es un registro de stock o un préstamo
    
    @ManyToOne
    @JoinColumn(name = "licoreria_id")
    private Licoreria licoreria;
    
    // Getters y Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public int getCantidad() {
        return cantidad;
    }
    
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
    
    public double getValorPorUnidad() {
        return valorPorUnidad;
    }
    
    public void setValorPorUnidad(double valorPorUnidad) {
        this.valorPorUnidad = valorPorUnidad;
    }
    
    public int getStockDisponible() {
        return stockDisponible;
    }
    
    public void setStockDisponible(int stockDisponible) {
        this.stockDisponible = stockDisponible;
    }
    
    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }
    
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }
    
    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }
    
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }
    
    public boolean isDevuelto() {
        return devuelto;
    }
    
    public void setDevuelto(boolean devuelto) {
        this.devuelto = devuelto;
    }
    
    public boolean isEsStock() {
        return esStock;
    }
    
    public void setEsStock(boolean esStock) {
        this.esStock = esStock;
    }
    
    public Licoreria getLicoreria() {
        return licoreria;
    }
    
    public void setLicoreria(Licoreria licoreria) {
        this.licoreria = licoreria;
    }
} 