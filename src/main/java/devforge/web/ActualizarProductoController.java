package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Producto;
import devforge.servicio.ProductoServicio;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/producto/actualizar")
public class ActualizarProductoController {

    private final ProductoServicio productoServicio;
    private final LicoreriaContext licoreriaContext;

    public ActualizarProductoController(ProductoServicio productoServicio, LicoreriaContext licoreriaContext) {
        this.productoServicio = productoServicio;
        this.licoreriaContext = licoreriaContext;
    }

    @GetMapping
    public String mostrarFormulario(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        List<Producto> productos = productoServicio.listarProductosPorLicoreria(licoreriaContext.getLicoreriaId())
            .stream()
            .filter(p -> p.isActivo())
            .toList();
        model.addAttribute("productos", productos);
        model.addAttribute("producto", new Producto());
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "actualizar";
    }

    @PostMapping
    public String actualizarProducto(@ModelAttribute Producto producto, RedirectAttributes redirectAttrs) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Debes seleccionar una licorería primero");
            return "redirect:/licorerias/seleccionar";
        }

        try {
            // Obtener el producto existente para mantener la licorería
            Producto productoExistente = productoServicio.obtenerPorId(producto.getId());
            if (productoExistente == null) {
                redirectAttrs.addFlashAttribute("mensajeError", "❌ Producto no encontrado");
                return "redirect:/producto/actualizar";
            }

            // Mantener la licorería actual
            producto.setLicoreria(licoreriaContext.getLicoreriaActual());
            
            // Actualizar el producto
            productoServicio.guardar(producto);
            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Producto actualizado exitosamente");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error al actualizar el producto: " + e.getMessage());
        }
        return "redirect:/producto/actualizar";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public List<Producto> buscarProductos(
            @RequestParam(required = false) String termino,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax) {
        
        List<Producto> resultados = productoServicio.buscarPorNombreOCodigo(termino)
            .stream()
            .filter(p -> p.isActivo())
            .toList();
        
        // Aplicar filtros adicionales
        if (categoria != null && !categoria.isEmpty()) {
            resultados = resultados.stream()
                .filter(p -> p.getCategoria().equalsIgnoreCase(categoria))
                .toList();
        }
        
        if (marca != null && !marca.isEmpty()) {
            resultados = resultados.stream()
                .filter(p -> p.getMarca().equalsIgnoreCase(marca))
                .toList();
        }
        
        if (precioMin != null) {
            resultados = resultados.stream()
                .filter(p -> p.getPrecioVenta() >= precioMin)
                .toList();
        }
        
        if (precioMax != null) {
            resultados = resultados.stream()
                .filter(p -> p.getPrecioVenta() <= precioMax)
                .toList();
        }
        
        return resultados;
    }
}
