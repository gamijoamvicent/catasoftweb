package devforge.servicio;

import devforge.model.Licoreria;
import devforge.repository.LicoreriaRepository;
import devforge.repository.ConfiguracionImpresoraRepository;
import devforge.repository.DetalleVentaRepository;
import devforge.repository.PrecioDolarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LicoreriaServicioImpl implements LicoreriaServicio {
    private static final Logger logger = LoggerFactory.getLogger(LicoreriaServicioImpl.class);

    private final LicoreriaRepository licoreriaRepository;
    private final VentaServicio ventaServicio;
    private final CreditoServicio creditoServicio;
    private final ClienteServicio clienteServicio;
    private final ProductoServicio productoServicio;
    private final UsuarioServicio usuarioServicio;
    private final ConfiguracionImpresoraRepository configuracionImpresoraRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PrecioDolarRepository precioDolarRepository;

    @Autowired
    public LicoreriaServicioImpl(LicoreriaRepository licoreriaRepository, VentaServicio ventaServicio, CreditoServicio creditoServicio, ClienteServicio clienteServicio, ProductoServicio productoServicio, UsuarioServicio usuarioServicio, ConfiguracionImpresoraRepository configuracionImpresoraRepository, DetalleVentaRepository detalleVentaRepository, PrecioDolarRepository precioDolarRepository) {
        this.licoreriaRepository = licoreriaRepository;
        this.ventaServicio = ventaServicio;
        this.creditoServicio = creditoServicio;
        this.clienteServicio = clienteServicio;
        this.productoServicio = productoServicio;
        this.usuarioServicio = usuarioServicio;
        this.configuracionImpresoraRepository = configuracionImpresoraRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.precioDolarRepository = precioDolarRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Licoreria> listarTodas() {
        logger.debug("Listando todas las licorerías");
        return licoreriaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Licoreria> listarActivas() {
        logger.debug("Listando licorerías activas");
        return licoreriaRepository.findByEstadoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Licoreria> obtenerPorId(Long id) {
        logger.debug("Buscando licorería por ID: {}", id);
        return licoreriaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Licoreria> obtenerPorNombre(String nombre) {
        logger.debug("Buscando licorería por nombre: {}", nombre);
        return licoreriaRepository.findByNombre(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Licoreria> obtenerPorIpLocal(String ipLocal) {
        logger.debug("Buscando licorería por IP local: {}", ipLocal);
        return licoreriaRepository.findByIpLocal(ipLocal);
    }

    @Override
    @Transactional
    public Licoreria guardar(Licoreria licoreria) {
        logger.debug("Guardando licorería: {}", licoreria.getNombre());
        if (licoreria.getId() == null) {
            licoreria.setEstado(true);
        }
        return licoreriaRepository.save(licoreria);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        logger.debug("Eliminando licorería ID: {}", id);
        licoreriaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long id, boolean estado) {
        logger.debug("Cambiando estado de licorería ID: {} a {}", id, estado);
        licoreriaRepository.findById(id).ifPresent(licoreria -> {
            licoreria.setEstado(estado);
            licoreriaRepository.save(licoreria);
        });
    }

    @Override
    @Transactional
    public void eliminarLicoreriaCompleta(Long licoreriaId) {
        try {
            logger.info("Iniciando eliminación completa de licorería ID: {}", licoreriaId);
            
            // Verificar que la licorería existe
            Licoreria licoreria = licoreriaRepository.findById(licoreriaId)
                .orElseThrow(() -> new RuntimeException("Licorería no encontrada con ID: " + licoreriaId));

            // 1. Eliminar configuración de impresora
            logger.info("Eliminando configuración de impresora de la licorería");
            try {
                configuracionImpresoraRepository.deleteByLicoreriaId(licoreriaId);
                configuracionImpresoraRepository.flush();
            } catch (Exception e) {
                logger.error("Error al eliminar configuración de impresora: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar configuración de impresora: " + e.getMessage(), e);
            }

            // 2. Eliminar créditos
            logger.info("Eliminando créditos de la licorería");
            try {
                creditoServicio.eliminarCreditosPorLicoreria(licoreriaId);
            } catch (Exception e) {
                logger.error("Error al eliminar créditos: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar créditos: " + e.getMessage(), e);
            }

            // 3. Eliminar detalles de venta
            logger.info("Eliminando detalles de venta de la licorería");
            try {
                detalleVentaRepository.deleteByLicoreriaId(licoreriaId);
                detalleVentaRepository.flush();
            } catch (Exception e) {
                logger.error("Error al eliminar detalles de venta: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar detalles de venta: " + e.getMessage(), e);
            }

            // 4. Eliminar ventas
            logger.info("Eliminando ventas de la licorería");
            try {
                ventaServicio.eliminarVentasPorLicoreria(licoreriaId);
            } catch (Exception e) {
                logger.error("Error al eliminar ventas: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar ventas: " + e.getMessage(), e);
            }

            // 5. Eliminar clientes
            logger.info("Eliminando clientes de la licorería");
            try {
                clienteServicio.eliminarClientesPorLicoreria(licoreriaId);
            } catch (Exception e) {
                logger.error("Error al eliminar clientes: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar clientes: " + e.getMessage(), e);
            }

            // 6. Eliminar precios de dólar
            logger.info("Eliminando precios de dólar de la licorería");
            try {
                precioDolarRepository.deleteByLicoreriaId(licoreriaId);
                precioDolarRepository.flush();
            } catch (Exception e) {
                logger.error("Error al eliminar precios de dólar: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar precios de dólar: " + e.getMessage(), e);
            }

            // 7. Eliminar productos
            logger.info("Eliminando productos de la licorería");
            try {
                productoServicio.eliminarProductosPorLicoreria(licoreriaId);
            } catch (Exception e) {
                logger.error("Error al eliminar productos: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar productos: " + e.getMessage(), e);
            }

            // 8. Eliminar usuarios (excepto SUPER_ADMIN)
            logger.info("Eliminando usuarios de la licorería");
            try {
                usuarioServicio.eliminarUsuariosPorLicoreria(licoreriaId);
            } catch (Exception e) {
                logger.error("Error al eliminar usuarios: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar usuarios: " + e.getMessage(), e);
            }

            // 9. Limpiar referencias antes de eliminar la licorería
            logger.info("Limpiando referencias de la licorería");
            try {
                licoreriaRepository.flush();
            } catch (Exception e) {
                logger.error("Error al limpiar referencias: {}", e.getMessage());
                throw new RuntimeException("Error al limpiar referencias: " + e.getMessage(), e);
            }

            // 10. Finalmente, eliminar la licorería
            logger.info("Eliminando la licorería");
            try {
                licoreriaRepository.delete(licoreria);
                licoreriaRepository.flush();
            } catch (Exception e) {
                logger.error("Error al eliminar la licorería: {}", e.getMessage());
                throw new RuntimeException("Error al eliminar la licorería: " + e.getMessage(), e);
            }
            
            logger.info("Eliminación completa de licorería finalizada exitosamente");
        } catch (Exception e) {
            logger.error("Error al eliminar licorería: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar la licorería: " + e.getMessage(), e);
        }
    }
} 