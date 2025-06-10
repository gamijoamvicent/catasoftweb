package devforge.servicio;

import devforge.model.Vacio;
import java.util.List;

public interface VacioService {
    Vacio registrarPrestamo(Vacio vacio);
    Vacio registrarDevolucion(Long id);
    List<Vacio> obtenerVaciosPrestados();
    List<Vacio> obtenerVaciosDevueltos();
    Vacio obtenerStock();
    Vacio actualizarStock(Long id, int nuevaCantidad);
    Vacio actualizarValor(Long id, double nuevoValor);
} 