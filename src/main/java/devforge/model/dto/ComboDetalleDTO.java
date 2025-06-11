package devforge.model.dto;

import java.math.BigDecimal;
import java.util.List;

public class ComboDetalleDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private List<ItemDTO> productos;
    private boolean requiereActualizacion;
    private String mensajeActualizacion;

    public static class ItemDTO {
        private Long id;
        private String nombre;
        private BigDecimal precio;
        private Integer cantidad;
        private boolean activo;
        private String mensajeEstado;

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

        public boolean isActivo() {
            return activo;
        }

        public void setActivo(boolean activo) {
            this.activo = activo;
        }

        public String getMensajeEstado() {
            return mensajeEstado;
        }

        public void setMensajeEstado(String mensajeEstado) {
            this.mensajeEstado = mensajeEstado;
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

    public boolean isRequiereActualizacion() {
        return requiereActualizacion;
    }

    public void setRequiereActualizacion(boolean requiereActualizacion) {
        this.requiereActualizacion = requiereActualizacion;
    }

    public String getMensajeActualizacion() {
        return mensajeActualizacion;
    }

    public void setMensajeActualizacion(String mensajeActualizacion) {
        this.mensajeActualizacion = mensajeActualizacion;
    }
}
