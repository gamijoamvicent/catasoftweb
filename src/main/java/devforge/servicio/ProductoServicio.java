/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package devforge.servicio;

import devforge.model.ItemVentaDto;
import devforge.model.Producto;
import java.util.List;

/**
 *
 * @author yraid
 */
public interface ProductoServicio {
    
    public void guardar(Producto producto);
    
    public List<Producto> listarProductos();
    
    public void eliminar(Long id);
    
    public void descontarStock(Long id, int cantidadVendida);
    Producto obtenerPorId(Long id);
    List<Producto> buscarPorNombreOCodigo(String termino);
    
    void descontarStockk(List<ItemVentaDto> items);
  
    double obtenerPrecioActual();
    
    List<Producto> listarProductosPorLicoreria(Long licoreriaId);
    List<Producto> listarProductosPorLicoreriaYCategoria(Long licoreriaId, String categoria);
    void eliminarProductosPorLicoreria(Long licoreriaId);
    
    // Métodos para ingreso de stock
    List<Producto> obtenerTodos();
    void registrarIngresoStock(Long productoId, Integer cantidad, String motivo);
}
