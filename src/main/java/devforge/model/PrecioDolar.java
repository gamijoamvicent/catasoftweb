/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package devforge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "preciodolar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecioDolar {

    public enum TipoTasa {
        BCV,
        PROMEDIO,
        PARALELA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_precio_dolar")
    private Long idPrecioDolar;

    @Column(name = "precio_dolar", nullable = false)
    private double precioDolar;

    @Column(name = "fecha_dolar")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaDolar;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tasa", nullable = false)
    private TipoTasa tipoTasa = TipoTasa.BCV;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licoreria_id", nullable = false)
    private Licoreria licoreria;

    @Column(name = "licoreria_id", insertable = false, updatable = false)
    private Long licoreriaId;
}
