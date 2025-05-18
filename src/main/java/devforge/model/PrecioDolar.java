/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package devforge.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "preciodolar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecioDolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPrecioDolar;
    private double precioDolar;
    @Column(name = "fecha_dolar")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaDolar;
}
