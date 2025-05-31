package devforge.servicio;

import devforge.model.Cliente;
import java.util.List;
import java.util.Optional;

public interface ClienteServicio {
    Cliente guardar(Cliente cliente);
    List<Cliente> listarPorLicoreria(Long licoreriaId);
    Optional<Cliente> buscarPorId(Long id);
    Optional<Cliente> buscarPorCedula(String cedula, Long licoreriaId);
    List<Cliente> buscarPorNombreOApellido(String termino, Long licoreriaId);
    void actualizarCreditoDisponible(Long clienteId, Double nuevoMonto);
    List<Cliente> listarClientesActivos(Long licoreriaId);
    void cambiarEstado(Long clienteId, boolean estado);
    void eliminarClientesPorLicoreria(Long licoreriaId);
} 