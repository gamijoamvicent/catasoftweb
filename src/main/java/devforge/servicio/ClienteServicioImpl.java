package devforge.servicio;

import devforge.model.Cliente;
import devforge.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClienteServicioImpl implements ClienteServicio {

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public Cliente guardar(Cliente cliente) {
        if (cliente.getId() == null) {
            cliente.setCreditoDisponible(cliente.getCreditoMaximo());
        }
        return clienteRepository.save(cliente);
    }

    @Override
    public List<Cliente> listarPorLicoreria(Long licoreriaId) {
        return clienteRepository.findByLicoreriaId(licoreriaId);
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    @Override
    public Optional<Cliente> buscarPorCedula(String cedula, Long licoreriaId) {
        return clienteRepository.findByCedulaAndLicoreria(cedula, licoreriaId);
    }

    @Override
    public List<Cliente> buscarPorNombreOApellido(String termino, Long licoreriaId) {
        return clienteRepository.findByNombreOrApellidoContaining(termino, licoreriaId);
    }

    @Override
    @Transactional
    public void actualizarCreditoDisponible(Long clienteId, Double nuevoMonto) {
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.setCreditoDisponible(nuevoMonto);
            clienteRepository.save(cliente);
        });
    }

    @Override
    public List<Cliente> listarClientesActivos(Long licoreriaId) {
        return clienteRepository.findByEstadoTrueAndLicoreria(licoreriaId);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long clienteId, boolean estado) {
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.setEstado(estado);
            clienteRepository.save(cliente);
        });
    }

    @Override
    @Transactional
    public void eliminarClientesPorLicoreria(Long licoreriaId) {
        List<Cliente> clientes = clienteRepository.findByLicoreriaId(licoreriaId);
        for (Cliente cliente : clientes) {
            clienteRepository.delete(cliente);
        }
    }
} 