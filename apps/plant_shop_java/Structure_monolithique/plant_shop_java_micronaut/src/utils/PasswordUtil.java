package util;

// Import de la librairie jBCrypt.
// Assurez-vous que le fichier jbcrypt-0.4.jar est dans votre dossier /lib
// et ajouté au classpath dans le Makefile.
import org.mindrot.jbcrypt.BCrypt;

/**
 * Classe utilitaire pour centraliser la gestion des mots de passe.
 * Elle fournit des méthodes statiques pour hacher les mots de passe
 * et vérifier leur correspondance avec un hash existant en utilisant l'algorithme BCrypt.
 * Cette classe n'est pas destinée à être instanciée.
 */
public final class PasswordUtil {

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     */
    private PasswordUtil() {}

    /**
     * Hache un mot de passe en clair en utilisant BCrypt.
     * Un "sel" (salt) est généré automatiquement par BCrypt.gensalt() et est inclus
     * dans le hash résultant, ce qui rend chaque hash unique même pour des mots de passe identiques.
     *
     * @param plainTextPassword Le mot de passe en clair à hacher.
     * @return Le hash du mot de passe (incluant le sel), prêt à être stocké en base de données.
     */
    public static String hashPassword(String plainTextPassword) {
        // BCrypt.gensalt() génère un sel avec un coût de travail (work factor) par défaut.
        // Un coût plus élevé rend le hachage plus lent et plus résistant aux attaques par force brute.
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
     * Vérifie si un mot de passe en clair correspond à un hash BCrypt stocké.
     * La méthode extrait automatiquement le sel du hash pour effectuer la comparaison.
     *
     * @param plainTextPassword Le mot de passe en clair fourni par l'utilisateur lors de la connexion.
     * @param hashedPassword    Le hash stocké en base de données pour cet utilisateur.
     * @return true si le mot de passe en clair correspond au hash, false sinon.
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        // Gère les cas où le mot de passe ou le hash seraient nuls pour éviter les erreurs.
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Cette exception peut être levée si le hash n'est pas dans un format BCrypt valide.
            // Il est plus sûr de retourner false dans ce cas.
            System.err.println("Avertissement : Tentative de vérification d'un hash non-BCrypt.");
            return false;
        }
    }
}
