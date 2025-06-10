package devforge.repository;

import devforge.model.Vacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VacioRepository extends JpaRepository<Vacio, Long> {
    List<Vacio> findByDevueltoFalse();
    List<Vacio> findByDevueltoTrue();
} 