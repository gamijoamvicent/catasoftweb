package devforge.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuraciones_impresora")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionImpresora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;

    @Column(name = "puerto_com")
    private String puertoCom;

    @Column(name = "deteccion_automatica")
    private Boolean deteccionAutomatica = false;

    @Column(name = "velocidad_baudios")
    private Integer velocidadBaudios = 9600;

    @Column(name = "bits_datos")
    private Integer bitsDatos = 8;

    @Column(name = "bits_parada")
    private Integer bitsParada = 1;

    @Column(name = "paridad")
    private String paridad = "NONE";

    @Column(name = "ancho_papel")
    private Integer anchoPapel = 80;

    @Column(name = "dpi")
    private Integer dpi = 203;

    @Column(name = "corte_automatico")
    private Boolean corteAutomatico = true;

    @Column(name = "imprimir_logo")
    private Boolean imprimirLogo = false;

    @Column(name = "ruta_logo")
    private String rutaLogo;

    @Column(name = "activa")
    private Boolean activa = false;

    @Column(name = "ticket_texto", columnDefinition = "TEXT")
    private String ticketTexto;
}
