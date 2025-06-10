package devforge.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestamos_vacios")
public class PrestamoVacio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private int cantidad;
    
    @Column(name = "valor_por_unidad", nullable = false)
    private double valorPorUnidad;
    
    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDateTime fechaPrestamo;
    
    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;
    
    @Column(nullable = false)
    private boolean devuelto;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
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