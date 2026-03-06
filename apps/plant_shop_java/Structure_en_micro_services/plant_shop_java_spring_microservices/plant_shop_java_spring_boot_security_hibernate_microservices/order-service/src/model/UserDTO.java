package model;

/**
 * DTO minimal pour représenter l'utilisateur authentifié
 * reconstruit à partir des headers X-User-Id et X-User-Admin
 */
public final class UserDTO {
    public final int id;
    public final boolean isAdmin;

    /**
     * Constructeur.
     * @param id int ID utilisateur
     * @param isAdmin boolean Est admin
     */
    public UserDTO(int id, boolean isAdmin) {
        this.id = id;
        this.isAdmin = isAdmin;
    }
}
