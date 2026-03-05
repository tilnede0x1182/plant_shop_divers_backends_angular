package repositories;

import models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository Spring Data JPA pour les utilisateurs.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    /** Trouve un utilisateur par email. */
    Optional<User> findByEmail(String email);
    /** Vérifie si un email existe déjà. */
    boolean existsByEmail(String email);
}
