package devforge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String descripcion;
    @Column(name = "codigo_unico", unique = true)
    private String codigoUnico;
    @Column(name = "precio_venta")
    private double precioVenta;
    @Column(name = "precio_costo")
    private double precioCosto;
    private String categoria;
    @Column(name = "marca")
    private String marca;
    private String proveedor;
    @Column(name = "cantidad")
    private int cantidad;
    private String color;
}
