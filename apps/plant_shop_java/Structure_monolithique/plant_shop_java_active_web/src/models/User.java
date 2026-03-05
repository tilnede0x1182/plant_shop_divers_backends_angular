package models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

/**
 * Modèle représentant un utilisateur.
 */
@Table("users")
public class User extends Model {
    static {
        validatePresenceOf("email", "password_hash");
        validateEmailOf("email");
    }

    /**
     * Indique si l utilisateur est admin.
     * @return boolean True si admin
     */
    public boolean isAdmin() {
        return this.getBoolean("is_admin");
    }
}
