package repositories;

import models.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository Spring Data JPA pour les articles de commande.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    /** Trouve les articles d'une commande. */
    List<OrderItem> findByOrderId(int orderId);
    /** Supprime les articles d'une commande. */
    void deleteByOrderId(int orderId);
}
