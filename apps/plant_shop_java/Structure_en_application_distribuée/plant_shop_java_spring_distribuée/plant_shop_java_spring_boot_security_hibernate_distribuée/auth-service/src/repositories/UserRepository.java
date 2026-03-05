package repository;

import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * Repository JPA pour les utilisateurs.
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    /**
     * Vérifie si un email existe déjà.
     * @param email Email à vérifier
     * @return true si l'email existe
     */
    boolean existsByEmail(String email);
}
