package devforge.servicio;

import devforge.model.PagoCredito;
import devforge.model.Credito;
import devforge.repository.PagoCreditoRepository;
import devforge.repository.CreditoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PagoCreditoServicioImpl implements PagoCreditoServicio {
    
    private final PagoCreditoRepository pagoCreditoRepository;
    private final CreditoRepository creditoRepository;
    private final CreditoServicio creditoServicio;
    
    public PagoCreditoServicioImpl(
            PagoCreditoRepository pagoCreditoRepository,
            CreditoRepository creditoRepository,
            CreditoServicio creditoServicio) {
        this.pagoCreditoRepository = pagoCreditoRepository;
        this.creditoRepository = creditoRepository;
        this.creditoServicio = creditoServicio;
    }
    
    @Override
    @Transactional
    public PagoCredito registrarPago(Long creditoId, BigDecimal monto,
                                    PagoCredito.MetodoPago metodoPago,
                                    String referencia, String observaciones) {
        Credito credito = creditoRepository.findById(creditoId)
            .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
            
        if (monto.compareTo(credito.getSaldoPendiente()) > 0) {
            throw new RuntimeException("El monto del pago no puede ser mayor al saldo pendiente");
        }
        
        PagoCredito pago = new PagoCredito();
        pago.setCredito(credito);
        pago.setLicoreria(credito.getLicoreria());
        pago.setMonto(monto);
        pago.setFechaPago(LocalDateTime.now());
        pago.setMetodoPago(metodoPago);
        pago.setReferencia(referencia);
        pago.setObservaciones(observaciones);
        
        // Actualizar el saldo del crédito
        credito.setMontoPagado(credito.getMontoPagado().add(monto));
        credito.setSaldoPendiente(credito.getSaldoPendiente().subtract(monto));
        
        // Actualizar el estado del crédito
        if (credito.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
            credito.setEstado(Credito.EstadoCredito.PAGADO_TOTAL);
        } else {
            credito.setEstado(Credito.EstadoCredito.PAGADO_PARCIAL);
        }
        
        creditoRepository.save(credito);
        return pagoCreditoRepository.save(pago);
    }

    @Override
    public List<PagoCredito> listarPagosPorCredito(Long creditoId) {
        return pagoCreditoRepository.findByCreditoId(creditoId);
    }

    @Override
    public List<PagoCredito> listarPagosPorLicoreria(Long licoreriaId) {
        return pagoCreditoRepository.findByLicoreriaId(licoreriaId);
    }

    @Override
    public List<PagoCredito> listarPagosPorFecha(Long licoreriaId, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return pagoCreditoRepository.findByLicoreriaIdAndFechaPagoBetween(licoreriaId, fechaInicio, fechaFin);
    }
} 