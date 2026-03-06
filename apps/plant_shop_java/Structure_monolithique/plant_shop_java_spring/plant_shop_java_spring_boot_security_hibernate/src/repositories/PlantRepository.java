package repositories;

import models.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA pour les plantes.
 */
@Repository
public interface PlantRepository extends JpaRepository<Plant, Integer> {
    List<Plant> findAllByOrderByNameAsc();
}
