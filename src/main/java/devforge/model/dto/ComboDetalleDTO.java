package devforge.model.dto;

import java.math.BigDecimal;
import java.util.List;

public class ComboDetalleDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private List<ItemDTO> productos;

    public static class ItemDTO {
        private Long id;
        private String nombre;
        private BigDecimal precio;
        private Integer cantidad;

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

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }
    }

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

    public List<ItemDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<ItemDTO> productos) {
        this.productos = productos;
    }
}
