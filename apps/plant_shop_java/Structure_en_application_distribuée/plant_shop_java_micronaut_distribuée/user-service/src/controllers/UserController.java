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
import models.User;
import repositories.UserRepository;
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
	public List<?> listAdmin(HttpRequest<?> request) throws Exception {
		return listImpl(request);
	}

	@Get("/users")
	public List<?> listAlias(HttpRequest<?> request) throws Exception {
		return listImpl(request);
	}

	@Get("/users/{id}")
	public HttpResponse<?> show(@PathVariable int id, HttpRequest<?> request) throws Exception {
		User currentUser = Guards.requireUser(request);
		if (currentUser.id != id && !currentUser.isAdmin) return HttpResponse.status(HttpStatus.FORBIDDEN);
		User user = repo.find(id);
		return user != null ? HttpResponse.ok(ApiMapper.toUser(user)) : HttpResponse.notFound();
	}

	@Post("/users")
	public HttpResponse<?> create(@Body User userData, HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		if (repo.findByEmailWithPassword(userData.email) != null) return HttpResponse.status(HttpStatus.CONFLICT);
		userData.passwordHash = PasswordUtil.hashPassword(userData.passwordHash);
		int newId = repo.create(userData);
		return HttpResponse.created(ApiMapper.toUser(repo.find(newId)));
	}

	@Patch("/users/{id}")
	public HttpResponse<?> updateUser(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
		return updateImpl(id, updatedData, request);
	}

	@Patch("/admin/users/{id}")
	public HttpResponse<?> updateAdminAlias(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
		return updateImpl(id, updatedData, request);
	}

	@Delete("/users/{id}")
	public HttpResponse<?> destroyUser(@PathVariable int id, HttpRequest<?> request) throws Exception {
		return destroyImpl(id, request);
	}

	@Delete("/admin/users/{id}")
	public HttpResponse<?> destroyAdminAlias(@PathVariable int id, HttpRequest<?> request) throws Exception {
		return destroyImpl(id, request);
	}

	private List<?> listImpl(HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		return repo.list().stream()
			.sorted(userComparator())
			.map(ApiMapper::toUser)
			.collect(Collectors.toList());
	}

	private HttpResponse<?> updateImpl(int id, User updatedData, HttpRequest<?> request) throws Exception {
		User currentUser = Guards.requireUser(request);
		if (currentUser.id != id && !currentUser.isAdmin) return HttpResponse.status(HttpStatus.FORBIDDEN);
		User existing = repo.find(id);
		if (existing == null) return HttpResponse.notFound();
		if (updatedData.name != null) existing.name = updatedData.name;
		if (updatedData.email != null) existing.email = updatedData.email;
		if (currentUser.isAdmin && updatedData.isAdmin != existing.isAdmin) existing.isAdmin = updatedData.isAdmin;
		repo.update(existing);
		return HttpResponse.ok(ApiMapper.toUser(repo.find(id)));
	}

	private HttpResponse<?> destroyImpl(int id, HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		repo.delete(id);
		return HttpResponse.ok();
	}

	private Comparator<User> userComparator() {
		return Comparator.comparing((User u) -> !u.isAdmin)
			.thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
	}
}
