package devforge.web;

import devforge.model.Producto;
import devforge.servicio.ProductoServicio;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class InventarioController {

    @Autowired
    private ProductoServicio productoServicio;

    
    @GetMapping("/inventario")
    public String mostrarInventario(Model model) {
        List<Producto> productos = productoServicio.listarProductos();
        model.addAttribute("productos", productos);
        return "inventario";
    }
    
    @GetMapping("/inventario/productos")
    @ResponseBody
    public List<Producto> obtenerProductos() {
        return productoServicio.listarProductos();
    }
    @GetMapping("/inventario/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(@RequestParam String termino) {
        return productoServicio.buscarPorNombreOCodigo(termino);
    }
}
