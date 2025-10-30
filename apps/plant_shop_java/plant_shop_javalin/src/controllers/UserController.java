// src/controllers/UserController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import java.sql.Connection;
import java.util.List;
import model.User;
import repository.UserRepository;
import util.PasswordUtil;

public final class UserController {

    private final UserRepository repo;

    public UserController(Connection db) {
        this.repo = new UserRepository(db);
    }

    public void list(Context ctx) throws Exception {
        List<User> users = repo.list();
        ctx.json(users);
    }

    public void show(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        User currentUser = ctx.attribute("user");
        if (currentUser.id != id && !currentUser.isAdmin) {
            throw new ForbiddenResponse();
        }
        User user = repo.find(id);
        if (user == null) throw new NotFoundResponse();
        ctx.json(user);
    }

    public void create(Context ctx) throws Exception {
        User data = ctx.bodyAsClass(User.class);
        data.passwordHash = PasswordUtil.hashPassword(data.passwordHash); // Le champ est réutilisé
        int newId = repo.create(data);
        data.id = newId;
        data.passwordHash = null; // Ne pas renvoyer le hash
        ctx.status(HttpStatus.CREATED).json(data);
    }

    public void update(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        User currentUser = ctx.attribute("user");
        if (currentUser.id != id && !currentUser.isAdmin) {
            throw new ForbiddenResponse();
        }
        User userToUpdate = repo.find(id);
        if (userToUpdate == null) throw new NotFoundResponse();

        User data = ctx.bodyAsClass(User.class);
        if (data.name != null) userToUpdate.name = data.name;
        if (data.email != null) userToUpdate.email = data.email;
        if (currentUser.isAdmin) { // Seul un admin peut changer le statut admin
            userToUpdate.isAdmin = data.isAdmin;
        }
        repo.update(userToUpdate);
        ctx.json(userToUpdate);
    }

    public void destroy(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        repo.delete(id);
        ctx.status(HttpStatus.OK);
    }
}
