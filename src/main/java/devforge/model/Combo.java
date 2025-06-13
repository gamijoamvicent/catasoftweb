package devforge.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "combos")
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(name = "tipo_tasa", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoTasa tipoTasa;

    @ManyToOne
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "creado_por", nullable = true) // Cambiar a nullable = true
    private Usuario creadoPor;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComboProducto> productos = new ArrayList<>();

    public enum TipoTasa {
        BCV,
        PARALELO,
        PROMEDIO
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public TipoTasa getTipoTasa() {
        return tipoTasa;
    }

    public void setTipoTasa(TipoTasa tipoTasa) {
        this.tipoTasa = tipoTasa;
    }

    public Licoreria getLicoreria() {
        return licoreria;
    }

    public void setLicoreria(Licoreria licoreria) {
        this.licoreria = licoreria;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<ComboProducto> getProductos() {
        return productos;
    }

    public void setProductos(List<ComboProducto> productos) {
        this.productos = productos;
    }

    public Usuario getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Usuario creadoPor) {
        this.creadoPor = creadoPor;
    }
} 