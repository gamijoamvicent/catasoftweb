/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package devforge.repository;

import devforge.model.PrecioDolar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author yraid
 */
@Repository
public interface PrecioDolarRepository extends JpaRepository<PrecioDolar, Long> {
    List<PrecioDolar> findAll();
    List<PrecioDolar> findByLicoreriaIdOrderByFechaDolarDesc(Long licoreriaId);
    List<PrecioDolar> findByLicoreriaIdAndTipoTasaOrderByFechaDolarDesc(Long licoreriaId, PrecioDolar.TipoTasa tipoTasa);
    
    @Modifying
    @Query("DELETE FROM PrecioDolar p WHERE p.licoreria.id = :licoreriaId")
    void deleteByLicoreriaId(Long licoreriaId);
}
