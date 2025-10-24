package dto;

import java.sql.Timestamp;

/** Représentation JSON d'un utilisateur (sans password). */
public final class UserDTO {
    public int       id;
    public String    name;
    public String    email;
    public boolean   isAdmin;
    public Timestamp createdAt;   // peut être null à la création

    public UserDTO() {}                       // constructeur vide (JSON ⇆ objet)
    public UserDTO(int id,String name,String email,boolean isAdmin,Timestamp ts){
        this.id=id; this.name=name; this.email=email; this.isAdmin=isAdmin; this.createdAt=ts;
    }
}
