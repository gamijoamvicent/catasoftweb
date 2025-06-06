package devforge.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ComboDTO {
    private String nombre;
    private BigDecimal precio;
    private List<Long> productoIds;
} 