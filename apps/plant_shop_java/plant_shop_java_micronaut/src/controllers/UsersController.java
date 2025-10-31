package controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import model.User;
import repository.UserRepository;
import security.Guards;
import util.ApiMapper;
import util.PasswordUtil;

@Controller("/api")
public class UserController {

    private final UserRepository repo;

    @Inject
    public UserController(Connection db) {
        this.repo = new UserRepository(db);
    }

    @Get("/admin/users")
    @Get("/users")
    public List<?> list(HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        return repo.list().stream()
            .sorted(userComparator())
            .map(ApiMapper::toUser)
            .collect(Collectors.toList());
    }

    @Get("/users/{id}")
    public HttpResponse<?> show(@PathVariable int id, HttpRequest<?> request) throws Exception {
        User currentUser = Guards.requireUser(request);
        if (currentUser.id != id && !currentUser.isAdmin) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        User user = repo.find(id);
        return user != null ? HttpResponse.ok(ApiMapper.toUser(user)) : HttpResponse.notFound();
    }

    @Post("/users")
    public HttpResponse<?> create(@Body User userData, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        if (repo.findByEmailWithPassword(userData.email) != null) {
            return HttpResponse.status(HttpStatus.CONFLICT);
        }
        userData.passwordHash = PasswordUtil.hashPassword(userData.passwordHash);
        int id = repo.create(userData);
        return HttpResponse.created(ApiMapper.toUser(repo.find(id)));
    }

    @Patch("/users/{id}")
    @Patch("/admin/users/{id}")
    public HttpResponse<?> update(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
        User currentUser = Guards.requireUser(request);
        if (currentUser.id != id && !currentUser.isAdmin) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }

        User existing = repo.find(id);
        if (existing == null) return HttpResponse.notFound();

        if (updatedData.name != null) existing.name = updatedData.name;
        if (updatedData.email != null) existing.email = updatedData.email;

        // Seul un admin peut changer le statut admin d'un autre utilisateur
        if (currentUser.isAdmin && updatedData.isAdmin != existing.isAdmin) {
            existing.isAdmin = updatedData.isAdmin;
        }

        repo.update(existing);
        return HttpResponse.ok(ApiMapper.toUser(repo.find(id)));
    }

    @Delete("/users/{id}")
    @Delete("/admin/users/{id}")
    public HttpResponse<?> destroy(@PathVariable int id, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        repo.delete(id);
        return HttpResponse.ok();
    }

    private Comparator<User> userComparator() {
        return Comparator.comparing((User u) -> !u.isAdmin)
                         .thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
