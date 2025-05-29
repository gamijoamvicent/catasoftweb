package devforge.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "clientes")
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10)
    private String cedula;
    
    @Column(nullable = false)
    private String nombre;
    
    @Column(nullable = false)
    private String apellido;
    
    @Column(length = 15)
    private String telefono;
    
    @Column(columnDefinition = "TEXT")
    private String direccion;
    
    @Column(name = "credito_maximo", nullable = false)
    private Double creditoMaximo;
    
    @Column(name = "credito_disponible", nullable = false)
    private Double creditoDisponible;
    
    @Column(nullable = false)
    private boolean estado = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Licoreria licoreria;
    
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Credito> creditos = new ArrayList<>();
    
    @OneToMany(mappedBy = "cliente")
    @JsonIgnore
    private List<Venta> ventas = new ArrayList<>();

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Double getCreditoMaximo() {
        return creditoMaximo;
    }

    public void setCreditoMaximo(Double creditoMaximo) {
        this.creditoMaximo = creditoMaximo;
    }

    public Double getCreditoDisponible() {
        return creditoDisponible;
    }

    public void setCreditoDisponible(Double creditoDisponible) {
        this.creditoDisponible = creditoDisponible;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public Licoreria getLicoreria() {
        return licoreria;
    }

    public void setLicoreria(Licoreria licoreria) {
        this.licoreria = licoreria;
    }

    public List<Credito> getCreditos() {
        return creditos;
    }

    public void setCreditos(List<Credito> creditos) {
        this.creditos = creditos;
    }

    public List<Venta> getVentas() {
        return ventas;
    }

    public void setVentas(List<Venta> ventas) {
        this.ventas = ventas;
    }

    public Long getLicoreriaId() {
        return licoreria != null ? licoreria.getId() : null;
    }
}