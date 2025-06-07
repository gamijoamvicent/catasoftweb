package devforge.web;

import devforge.config.LicoreriaContext;
import devforge.model.Licoreria;
import devforge.model.Producto;
import devforge.repository.ProductoRepository;
import devforge.servicio.LicoreriaServicio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/producto")
public class ProductoController {

    private final ProductoRepository productoRepositorio;
    private final LicoreriaContext licoreriaContext;
    private final LicoreriaServicio licoreriaServicio;

    public ProductoController(ProductoRepository productoRepositorio, 
                            LicoreriaContext licoreriaContext,
                            LicoreriaServicio licoreriaServicio) {
        this.productoRepositorio = productoRepositorio;
        this.licoreriaContext = licoreriaContext;
        this.licoreriaServicio = licoreriaServicio;
    }

    @GetMapping("/agregar")
    public String mostrarFormulario(Model model) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }
        model.addAttribute("producto", new Producto());
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        return "agregarProducto";
    }

    @GetMapping("/listar")
    public String listarProductos(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            Model model) {
        
        if (licoreriaContext.getLicoreriaActual() == null) {
            return "redirect:/licorerias/seleccionar";
        }

        List<Producto> productos = productoRepositorio.findByLicoreriaId(licoreriaContext.getLicoreriaId());
        
        // Aplicar filtros
        if (categoria != null && !categoria.isEmpty()) {
            productos = productos.stream()
                .filter(p -> p.getCategoria().equalsIgnoreCase(categoria))
                .toList();
        }
        
        if (marca != null && !marca.isEmpty()) {
            productos = productos.stream()
                .filter(p -> p.getMarca().equalsIgnoreCase(marca))
                .toList();
        }
        
        if (proveedor != null && !proveedor.isEmpty()) {
            productos = productos.stream()
                .filter(p -> p.getProveedor().equalsIgnoreCase(proveedor))
                .toList();
        }
        
        if (precioMin != null) {
            productos = productos.stream()
                .filter(p -> p.getPrecioVenta() >= precioMin)
                .toList();
        }
        
        if (precioMax != null) {
            productos = productos.stream()
                .filter(p -> p.getPrecioVenta() <= precioMax)
                .toList();
        }

        // Obtener opciones únicas para los filtros
        List<String> categorias = productoRepositorio.findByLicoreriaId(licoreriaContext.getLicoreriaId())
            .stream()
            .map(Producto::getCategoria)
            .distinct()
            .toList();
            
        List<String> marcas = productoRepositorio.findByLicoreriaId(licoreriaContext.getLicoreriaId())
            .stream()
            .map(Producto::getMarca)
            .distinct()
            .toList();
            
        List<String> proveedores = productoRepositorio.findByLicoreriaId(licoreriaContext.getLicoreriaId())
            .stream()
            .map(Producto::getProveedor)
            .distinct()
            .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("licoreriaActual", licoreriaContext.getLicoreriaActual());
        model.addAttribute("categorias", categorias);
        model.addAttribute("marcas", marcas);
        model.addAttribute("proveedores", proveedores);
        model.addAttribute("categoriaSeleccionada", categoria);
        model.addAttribute("marcaSeleccionada", marca);
        model.addAttribute("proveedorSeleccionado", proveedor);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);
        
        return "listarProductos";
    }

    @PostMapping("/agregar")
    public String guardarProducto(
            @ModelAttribute Producto producto,
            @RequestParam(name = "colores", required = false) String coloresStr,
            RedirectAttributes redirectAttrs) {

        if (licoreriaContext.getLicoreriaActual() == null) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Debes seleccionar una licorería primero");
            return "redirect:/licorerias/seleccionar";
        }

        try {
            String nombre = producto.getNombre();
            String descripcion = producto.getDescripcion();
            double precioVenta = producto.getPrecioVenta();
            double precioCosto = producto.getPrecioCosto();
            String categoria = producto.getCategoria();
            String marca = producto.getMarca();
            String proveedor = producto.getProveedor();
            int cantidad = producto.getCantidad();
            String tipoTasa = producto.getTipoTasa() != null ? producto.getTipoTasa() : "BCV";

            Licoreria licoreriaActual = licoreriaContext.getLicoreriaActual();

            // Convertimos el string a lista
            List<String> colores = new ArrayList<>();
            if (coloresStr != null && !coloresStr.isEmpty()) {
                colores = Arrays.asList(coloresStr.split(","));
            }

            if (colores.isEmpty()) {
                Producto p = new Producto();
                p.setNombre(nombre);
                p.setDescripcion(descripcion);
                p.setPrecioVenta(precioVenta);
                p.setPrecioCosto(precioCosto);
                p.setCategoria(categoria);
                p.setMarca(marca);
                p.setProveedor(proveedor);
                p.setCantidad(cantidad);
                p.setTipoTasa(tipoTasa);
                p.setLicoreria(licoreriaActual);
                productoRepositorio.save(p);
            } else {
                for (String color : colores) {
                    Producto p = new Producto();
                    p.setNombre(nombre + "-" + color.trim());
                    p.setDescripcion(descripcion);
                    p.setPrecioVenta(precioVenta);
                    p.setPrecioCosto(precioCosto);
                    p.setCategoria(categoria);
                    p.setMarca(marca);
                    p.setProveedor(proveedor);
                    p.setCantidad(cantidad);
                    p.setTipoTasa(tipoTasa);
                    p.setLicoreria(licoreriaActual);
                    productoRepositorio.save(p);
                }
            }

            redirectAttrs.addFlashAttribute("mensajeExito", "✅ Productos guardados con éxito");
        } catch (NumberFormatException e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Error: Debes ingresar números válidos en los campos numéricos.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("mensajeError", "❌ Ocurrió un error al guardar los productos: " + e.getMessage());
        }

        return "redirect:/producto/agregar";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public List<Map<String, Object>> buscarProductos(@RequestParam String termino) {
        if (licoreriaContext.getLicoreriaActual() == null) {
            return new ArrayList<>();
        }

        return productoRepositorio.findByLicoreriaId(licoreriaContext.getLicoreriaId())
            .stream()
            .filter(p -> p.isActivo())
            .filter(p -> p.getNombre().toLowerCase().contains(termino.toLowerCase()))
            .map(p -> {
                Map<String, Object> productoData = new HashMap<>();
                productoData.put("id", p.getId());
                productoData.put("nombre", p.getNombre());
                productoData.put("precioVenta", p.getPrecioVenta());
                productoData.put("cantidad", p.getCantidad());
                return productoData;
            })
            .toList();
    }
}