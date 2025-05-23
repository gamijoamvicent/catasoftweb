package devforge.model;

public class ItemVentaDto {
    private Long id;
    private int cantidad;

    public ItemVentaDto(Long id, int cantidad) {
        this.id = id;
        this.cantidad = cantidad;
    }

    public Long getId() {
        return id;
    }

    public int getCantidad() {
        return cantidad;
    }
}