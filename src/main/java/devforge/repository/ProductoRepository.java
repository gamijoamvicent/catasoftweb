/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package devforge.repository;

import devforge.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author yraid
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Override
    List<Producto> findAll();

    @Override
    Optional<Producto> findById(Long id);

    List<Producto> findByLicoreriaId(Long licoreriaId);
    List<Producto> findByLicoreriaIdAndCategoria(Long licoreriaId, String categoria);
}
