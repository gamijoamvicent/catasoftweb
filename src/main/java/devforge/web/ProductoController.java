package devforge.web;

import devforge.model.Producto;
import devforge.repository.ProductoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class ProductoController {

    private final ProductoRepository productoRepositorio;

    public ProductoController(ProductoRepository productoRepositorio) {
        this.productoRepositorio = productoRepositorio;
    }

    @GetMapping("/producto/agregar")
    public String mostrarFormulario(Model model) {
        model.addAttribute("producto", new Producto());
        return "agregarProducto";
    }

    @PostMapping("/producto/agregar")
    public String guardarProducto(
            @ModelAttribute Producto producto,
            @RequestParam(name = "colores", required = false) String coloresStr, // Aquí la corrección
            RedirectAttributes redirectAttrs) {

        try {
            String nombre = producto.getNombre();
            String descripcion = producto.getDescripcion();
            double precioVenta = producto.getPrecioVenta();
            double precioCosto = producto.getPrecioCosto();
            String categoria = producto.getCategoria();
            String marca = producto.getMarca();
            String proveedor = producto.getProveedor();
            int cantidad = producto.getCantidad();

            String codigoBase = generarCodigoBusqueda(nombre);

            // Convertimos el string a lista
            List<String> colores = new ArrayList<>();
            if (coloresStr != null && !coloresStr.isEmpty()) {
                colores = Arrays.asList(coloresStr.split(","));
            }

            if (colores.isEmpty()) {
                Producto p = new Producto();
                p.setNombre(nombre);
                p.setDescripcion(descripcion);
                p.setCodigoUnico(codigoBase);
                p.setPrecioVenta(precioVenta);
                p.setPrecioCosto(precioCosto);
                p.setCategoria(categoria);
                p.setMarca(marca);
                p.setProveedor(proveedor);
                p.setCantidad(cantidad);
                productoRepositorio.save(p);
            } else {
                for (String color : colores) {
                    Producto p = new Producto();
                    p.setNombre(nombre + "-" + color.trim());
                    p.setDescripcion(descripcion);
                    p.setCodigoUnico(codigoBase + "-" + generarCodigoColor(color.trim()));
                    p.setPrecioVenta(precioVenta);
                    p.setPrecioCosto(precioCosto);
                    p.setCategoria(categoria);
                    p.setMarca(marca);
                    p.setProveedor(proveedor);
                    p.setCantidad(cantidad);
                    productoRepositorio.save(p);
                }
            }

            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Productos guardados con éxito");
        } catch (NumberFormatException e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error: Debes ingresar números válidos en los campos numéricos.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Ocurrió un error al guardar los productos.");
        }

        return "redirect:/producto/agregar";
    }

    // Genera un código base a partir del nombre del producto
    private String generarCodigoBusqueda(String texto) {
        if (texto == null || texto.isEmpty()) return "";

        String[] palabras = texto.toUpperCase().split("\\s+");
        StringBuilder codigo = new StringBuilder();
        boolean primeraPalabraProcesada = false;

        for (String palabra : palabras) {
            if (palabra.equalsIgnoreCase("DE")) continue;

            int longitud = Math.min(3, palabra.length());

            if (primeraPalabraProcesada && codigo.length() > 0) {
                codigo.append("-");
            }

            codigo.append(palabra.substring(0, longitud));

            if (!primeraPalabraProcesada) {
                primeraPalabraProcesada = true;
            } else {
                break;
            }
        }

        return codigo.toString();
    }

    // Genera un código corto del color (máximo 3 letras)
    private String generarCodigoColor(String color) {
        if (color == null || color.isEmpty()) return "";
        return color.length() >= 3 ? color.substring(0, 3).toUpperCase() : color.toUpperCase();
    }
}