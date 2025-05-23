package devforge.web;

import devforge.model.Producto;
import devforge.servicio.ProductoServicio;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/producto/eliminar")
public class EliminarController {

    private final ProductoServicio productoServicio;

    public EliminarController(ProductoServicio productoServicio) {
        this.productoServicio = productoServicio;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        List<Producto> productos = productoServicio.listarProductos();
        model.addAttribute("productos", productos);
        return "eliminar";
    }

    @GetMapping("/{id}")
    public String eliminar(@PathVariable Long id) {
        productoServicio.eliminar(id);
        return "redirect:/producto/eliminar";
    }
}
