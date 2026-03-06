package repository;

import model.PlantStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA pour le stock des plantes.
 */
@Repository
public interface PlantRepository extends JpaRepository<PlantStock, Integer> {
}
