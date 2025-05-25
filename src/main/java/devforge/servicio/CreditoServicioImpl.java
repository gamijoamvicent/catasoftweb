package devforge.servicio;

import devforge.model.Cliente;
import devforge.model.Credito;
import devforge.model.Venta;
import devforge.repository.CreditoRepository;
import devforge.servicio.ClienteServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CreditoServicioImpl implements CreditoServicio {

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private ClienteServicio clienteServicio;

    @Override
    public Credito crearCredito(Venta venta) {
        if (venta.getTipoVenta() != Venta.TipoVenta.CREDITO || venta.getCliente() == null) {
            throw new IllegalArgumentException("La venta debe ser a crédito y tener un cliente asociado");
        }

        Credito credito = new Credito();
        credito.setVenta(venta);
        credito.setCliente(venta.getCliente());
        credito.setLicoreria(venta.getLicoreria());
        credito.setMontoTotal(venta.getTotalVenta());
        credito.setSaldoPendiente(venta.getTotalVenta());
        credito.setFechaLimitePago(LocalDateTime.now().plusDays(30)); // Por defecto 30 días
        
        // Actualizar crédito disponible del cliente
        Double creditoDisponible = venta.getCliente().getCreditoDisponible();
        Double montoVenta = venta.getTotalVenta().doubleValue();
        clienteServicio.actualizarCreditoDisponible(venta.getCliente().getId(), 
            creditoDisponible - montoVenta);

        return creditoRepository.save(credito);
    }

    @Override
    @Transactional
    public Credito registrarPago(Long creditoId, BigDecimal montoPago) {
        Credito credito = creditoRepository.findById(creditoId)
            .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));

        if (montoPago.compareTo(credito.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException("El monto del pago no puede ser mayor al saldo pendiente");
        }

        credito.setMontoPagado(credito.getMontoPagado().add(montoPago));
        credito.setSaldoPendiente(credito.getSaldoPendiente().subtract(montoPago));

        // Actualizar estado del crédito
        if (credito.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
            credito.setEstado(Credito.EstadoCredito.PAGADO_TOTAL);
        } else {
            credito.setEstado(Credito.EstadoCredito.PAGADO_PARCIAL);
        }

        // Actualizar crédito disponible del cliente
        Double creditoDisponible = credito.getCliente().getCreditoDisponible();
        clienteServicio.actualizarCreditoDisponible(credito.getCliente().getId(), 
            creditoDisponible + montoPago.doubleValue());

        return creditoRepository.save(credito);
    }

    @Override
    public List<Credito> listarCreditosPorCliente(Long clienteId, Long licoreriaId) {
        return creditoRepository.findByClienteAndLicoreria(clienteId, licoreriaId);
    }

    @Override
    public List<Credito> listarCreditosPendientes(Long licoreriaId) {
        return creditoRepository.findByEstadoAndLicoreria(Credito.EstadoCredito.PENDIENTE, licoreriaId);
    }

    @Override
    public Optional<Credito> buscarPorId(Long id) {
        return creditoRepository.findById(id);
    }

    @Override
    @Transactional
    public void actualizarEstadoCredito(Long creditoId) {
        creditoRepository.findById(creditoId).ifPresent(credito -> {
            LocalDateTime ahora = LocalDateTime.now();
            if (credito.getFechaLimitePago().isBefore(ahora) && 
                credito.getEstado() != Credito.EstadoCredito.PAGADO_TOTAL) {
                credito.setEstado(Credito.EstadoCredito.VENCIDO);
                creditoRepository.save(credito);
            }
        });
    }

    @Override
    public List<Credito> listarCreditosVencidos(Long licoreriaId) {
        return creditoRepository.findVencidosByLicoreria(licoreriaId);
    }

    @Override
    public List<Credito> listarCreditosPorLicoreria(Long licoreriaId) {
        return creditoRepository.findByLicoreriaId(licoreriaId);
    }
} 