package devforge.servicio;

import devforge.model.Cliente;
import devforge.model.Credito;
import devforge.model.Venta;
import devforge.model.enums.TipoVenta;
import devforge.repository.CreditoRepository;
import devforge.servicio.ClienteServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class CreditoServicioImpl implements CreditoServicio {

    @Autowired
    private CreditoRepository creditoRepository;

    @Autowired
    private ClienteServicio clienteServicio;

    @Override
    @Transactional
    public Credito crearCredito(Venta venta) {
        if (venta.getTipoVenta() != TipoVenta.CREDITO || venta.getCliente() == null) {
            throw new IllegalArgumentException("La venta debe ser a crédito y tener un cliente asociado");
        }

        Credito credito = new Credito();
        credito.setVenta(venta);
        credito.setCliente(venta.getCliente());
        credito.setLicoreria(venta.getLicoreria());
        credito.setMontoTotal(new BigDecimal(venta.getTotalVenta().toString()));
        credito.setSaldoPendiente(new BigDecimal(venta.getTotalVenta().toString()));
        credito.setFechaLimitePago(LocalDateTime.now().plusDays(30)); // Por defecto 30 días
        credito.setEstado(Credito.EstadoCredito.PENDIENTE);

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

    @Override
    public List<Credito> listarCreditosPorLicoreriaYEstado(Long licoreriaId, String estado) {
        try {
            Credito.EstadoCredito estadoCredito = Credito.EstadoCredito.valueOf(estado);
            return creditoRepository.findByLicoreriaIdAndEstado(licoreriaId, estadoCredito);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de crédito inválido: " + estado);
        }
    }

    @Override
    public Map<String, Double> obtenerEstadisticasCreditos(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Credito> creditos = creditoRepository.findByLicoreriaId(licoreriaId).stream()
            .filter(credito -> {
                LocalDateTime fechaVenta = credito.getVenta().getFechaVenta();
                return !fechaVenta.isBefore(fechaInicio) && !fechaVenta.isAfter(fechaFin);
            })
            .collect(Collectors.toList());

        Map<String, Double> estadisticas = new HashMap<>();
        
        // Total de créditos otorgados
        double totalCreditos = creditos.stream()
            .mapToDouble(c -> c.getMontoTotal().doubleValue())
            .sum();
        estadisticas.put("Total Créditos", totalCreditos);

        // Total pagado
        double totalPagado = creditos.stream()
            .mapToDouble(c -> c.getMontoPagado().doubleValue())
            .sum();
        estadisticas.put("Total Pagado", totalPagado);

        // Total pendiente
        double totalPendiente = creditos.stream()
            .mapToDouble(c -> c.getSaldoPendiente().doubleValue())
            .sum();
        estadisticas.put("Total Pendiente", totalPendiente);

        // Créditos vencidos
        double totalVencido = creditos.stream()
            .filter(c -> c.getEstado() == Credito.EstadoCredito.VENCIDO)
            .mapToDouble(c -> c.getSaldoPendiente().doubleValue())
            .sum();
        estadisticas.put("Total Vencido", totalVencido);

        return estadisticas;
    }

    @Override
    @Transactional
    public void eliminarCreditosPorLicoreria(Long licoreriaId) {
        List<Credito> creditos = creditoRepository.findByLicoreriaId(licoreriaId);
        for (Credito credito : creditos) {
            creditoRepository.delete(credito);
        }
    }
} 