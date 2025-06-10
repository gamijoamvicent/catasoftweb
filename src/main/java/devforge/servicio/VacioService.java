package devforge.servicio;

import devforge.model.Vacio;
import java.util.List;

public interface VacioService {
    Vacio registrarPrestamo(Vacio vacio);
    Vacio registrarDevolucion(Long id);
    List<Vacio> obtenerVaciosPrestados();
    List<Vacio> obtenerVaciosDevueltos();
    double obtenerMontoGarantiaActual();
} 