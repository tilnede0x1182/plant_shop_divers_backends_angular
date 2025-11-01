package models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("users")
public class User extends Model {
    static {
        validatePresenceOf("email", "password_hash");
        validateEmailOf("email");
    }

    public boolean isAdmin() {
        return this.getBoolean("is_admin");
    }
}
