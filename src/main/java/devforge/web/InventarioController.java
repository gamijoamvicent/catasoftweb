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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;

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

    @PostMapping("/inventario/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        try {
            productoServicio.eliminar(id);
            return ResponseEntity.ok().body("ok");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/inventario/exportar")
    public ResponseEntity<byte[]> exportarInventario() {
        List<Producto> productos = productoServicio.listarProductos();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Nombre,Precio Venta,Cantidad,Categoría,Marca,Proveedor\n");
        for (Producto p : productos) {
            sb.append(p.getId()).append(",")
              .append(p.getNombre()).append(",")
              .append(p.getPrecioVenta()).append(",")
              .append(p.getCantidad()).append(",")
              .append(p.getCategoria()).append(",")
              .append(p.getMarca()).append(",")
              .append(p.getProveedor()).append("\n");
        }
        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Inventario.csv");
        return ResponseEntity.ok().headers(headers).body(csvBytes);
    }
}
