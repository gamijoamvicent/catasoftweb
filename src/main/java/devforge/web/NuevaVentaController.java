package devforge.web;

import devforge.model.ItemVentaDto;
import devforge.model.Producto;
import devforge.model.ProductoCarritoDto;
import devforge.servicio.ProductoServicio;
import devforge.servicio.PrecioDolarServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.ResponseEntity;

@Controller

public class NuevaVentaController {

    @Autowired
    private ProductoServicio productoServicio;

    @Autowired
    private PrecioDolarServicio precioDolarServicio;

    @GetMapping("/ventas")
    public String mostrarFormulario(Model model) {
        List<Producto> productos = productoServicio.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("precioDolar", productoServicio.obtenerPrecioActual());
        return "ventas/nuevaVenta";
    }

    @PostMapping("/ventas/confirmar")
    public ResponseEntity<String> confirmarVenta(@RequestBody List<ItemVentaDto> items) {
        try {
            productoServicio.descontarStockk(items);
            return ResponseEntity.ok("Venta confirmada, stock actualizado");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al procesar la venta: " + e.getMessage());
        }
    }

    @GetMapping("/ventas/productos")
    @ResponseBody
    public List<Producto> obtenerProductos() {
        return productoServicio.listarProductos();
    }

    @GetMapping("/ventas/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(@RequestParam String termino) {
        return productoServicio.buscarPorNombreOCodigo(termino);
    }
}
