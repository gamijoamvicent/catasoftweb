package devforge.model.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class ComboDetalleDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private List<ItemDTO> productos;

    @Data
    public static class ItemDTO {
        private Long id;
        private String nombre;
        private BigDecimal precio;
        private int cantidad;
    }
}
