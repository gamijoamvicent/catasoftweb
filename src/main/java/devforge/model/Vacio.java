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
    private double montoGarantia;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucion;
    private boolean devuelto;
    
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
    
    public double getMontoGarantia() {
        return montoGarantia;
    }
    
    public void setMontoGarantia(double montoGarantia) {
        this.montoGarantia = montoGarantia;
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
    
    public Licoreria getLicoreria() {
        return licoreria;
    }
    
    public void setLicoreria(Licoreria licoreria) {
        this.licoreria = licoreria;
    }
} 