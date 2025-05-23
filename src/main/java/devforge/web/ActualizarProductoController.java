
package devforge.web;

import devforge.model.Producto;
import devforge.servicio.ProductoServicio;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/producto")
public class ActualizarProductoController {

    private final ProductoServicio productoServicio;

    public ActualizarProductoController(ProductoServicio productoServicio) {
        this.productoServicio = productoServicio;
    }

    @GetMapping("/actualizar")
    public String mostrarFormulario(Model model) {
        List<Producto> productos = productoServicio.listarProductos();
        model.addAttribute("productos", productos);
        model.addAttribute("producto", new Producto());
        return "actualizar";
    }

    @PostMapping("/actualizar")
    public String actualizarProducto(@ModelAttribute Producto producto, Model model) {
        try {
            productoServicio.guardar(producto);
            model.addAttribute("mensajeExito", "✅ Producto actualizado exitosamente.");
        } catch (Exception e) {
            model.addAttribute("mensajeError", "❌ Error al actualizar el producto.");
        }
        return "redirect:/producto/actualizar";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(@RequestParam String termino) {
        return productoServicio.buscarPorNombreOCodigo(termino);
    }
}
