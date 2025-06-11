package devforge.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import devforge.model.Venta;
import devforge.model.enums.TipoVenta;

/**
 * DTO para transferir información de ventas de cajas
 */
public class VentaCajaDTO {
    private Long id;
    private LocalDateTime fechaCreacion;
    private String tipoCaja;
    private String cajaNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private BigDecimal subtotalBolivares;
    private BigDecimal tasaCambioUsado;
    private String metodoPago;
    private String nombreCliente;
    private boolean activo = true;

    public VentaCajaDTO() {
    }

    public VentaCajaDTO(Long id, LocalDateTime fechaCreacion, String tipoCaja, String cajaNombre, 
                      Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal,
                      BigDecimal subtotalBolivares, BigDecimal tasaCambioUsado,
                      String metodoPago, String nombreCliente, boolean activo) {
        this.id = id;
        this.fechaCreacion = fechaCreacion;
        this.tipoCaja = tipoCaja;
        this.cajaNombre = cajaNombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.subtotalBolivares = subtotalBolivares;
        this.tasaCambioUsado = tasaCambioUsado;
        this.metodoPago = metodoPago;
        this.nombreCliente = nombreCliente;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getTipoCaja() {
        return tipoCaja;
    }

    public void setTipoCaja(String tipoCaja) {
        this.tipoCaja = tipoCaja;
    }

    public String getCajaNombre() {
        return cajaNombre;
    }

    public void setCajaNombre(String cajaNombre) {
        this.cajaNombre = cajaNombre;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getSubtotalBolivares() {
        return subtotalBolivares;
    }

    public void setSubtotalBolivares(BigDecimal subtotalBolivares) {
        this.subtotalBolivares = subtotalBolivares;
    }

    public BigDecimal getTasaCambioUsado() {
        return tasaCambioUsado;
    }

    public void setTasaCambioUsado(BigDecimal tasaCambioUsado) {
        this.tasaCambioUsado = tasaCambioUsado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public void actualizarNombreClienteSegunVenta(Venta venta) {
        if (venta != null) {
            if (venta.getTipoVenta() == TipoVenta.CREDITO && venta.getCliente() != null) {
                this.nombreCliente = venta.getCliente().getNombre();
            } else {
                this.nombreCliente = "Venta al contado";
            }
        }
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
