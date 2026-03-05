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

/**
 * Contrôleur pour les utilisateurs Micronaut.
 */
@Controller("/api")
public class UserController {

	private final UserRepository repo;

	/**
	 * Constructeur avec injection.
	 * @param db Connection Connexion DB
	 */
	@Inject
	public UserController(Connection db) {
		this.repo = new UserRepository(db);
	}

	/**
	 * Liste les utilisateurs (admin).
	 * @param request HttpRequest Requête HTTP
	 * @return List Liste d utilisateurs
	 * @throws Exception En cas d erreur
	 */
	@Get("/admin/users")
	public List<?> listAdmin(HttpRequest<?> request) throws Exception {
		return listImpl(request);
	}

	/**
	 * Liste les utilisateurs (alias).
	 * @param request HttpRequest Requête HTTP
	 * @return List Liste d utilisateurs
	 * @throws Exception En cas d erreur
	 */
	@Get("/users")
	public List<?> listAlias(HttpRequest<?> request) throws Exception {
		return listImpl(request);
	}

	/**
	 * Affiche un utilisateur.
	 * @param id int ID utilisateur
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	@Get("/users/{id}")
	public HttpResponse<?> show(@PathVariable int id, HttpRequest<?> request) throws Exception {
		User currentUser = Guards.requireUser(request);
		if (currentUser.id != id && !currentUser.isAdmin) return HttpResponse.status(HttpStatus.FORBIDDEN);
		User user = repo.find(id);
		return user != null ? HttpResponse.ok(ApiMapper.toUser(user)) : HttpResponse.notFound();
	}

	/**
	 * Crée un utilisateur.
	 * @param userData User Données utilisateur
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	@Post("/users")
	public HttpResponse<?> create(@Body User userData, HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		if (repo.findByEmailWithPassword(userData.email) != null) return HttpResponse.status(HttpStatus.CONFLICT);
		userData.passwordHash = PasswordUtil.hashPassword(userData.passwordHash);
		int newId = repo.create(userData);
		return HttpResponse.created(ApiMapper.toUser(repo.find(newId)));
	}

	/**
	 * Met à jour un utilisateur.
	 * @param id int ID utilisateur
	 * @param updatedData User Nouvelles données
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	@Patch("/users/{id}")
	public HttpResponse<?> updateUser(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
		return updateImpl(id, updatedData, request);
	}

	/**
	 * Met à jour un utilisateur (alias admin).
	 * @param id int ID utilisateur
	 * @param updatedData User Nouvelles données
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	@Patch("/admin/users/{id}")
	public HttpResponse<?> updateAdminAlias(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
		return updateImpl(id, updatedData, request);
	}

	/**
	 * Supprime un utilisateur.
	 * @param id int ID utilisateur
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	@Delete("/users/{id}")
	public HttpResponse<?> destroyUser(@PathVariable int id, HttpRequest<?> request) throws Exception {
		return destroyImpl(id, request);
	}

	/**
	 * Supprime un utilisateur (alias admin).
	 * @param id int ID utilisateur
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	@Delete("/admin/users/{id}")
	public HttpResponse<?> destroyAdminAlias(@PathVariable int id, HttpRequest<?> request) throws Exception {
		return destroyImpl(id, request);
	}

	/**
	 * Implémentation liste utilisateurs.
	 * @param request HttpRequest Requête HTTP
	 * @return List Liste d utilisateurs
	 * @throws Exception En cas d erreur
	 */
	private List<?> listImpl(HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		return repo.list().stream()
			.sorted(userComparator())
			.map(ApiMapper::toUser)
			.collect(Collectors.toList());
	}

	/**
	 * Implémentation mise à jour utilisateur.
	 * @param id int ID utilisateur
	 * @param updatedData User Nouvelles données
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
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

	/**
	 * Implémentation suppression utilisateur.
	 * @param id int ID utilisateur
	 * @param request HttpRequest Requête HTTP
	 * @return HttpResponse Réponse HTTP
	 * @throws Exception En cas d erreur
	 */
	private HttpResponse<?> destroyImpl(int id, HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		repo.delete(id);
		return HttpResponse.ok();
	}

	/**
	 * Retourne un comparateur pour trier les utilisateurs.
	 * @return Comparator Comparateur
	 */
	private Comparator<User> userComparator() {
		return Comparator.comparing((User u) -> !u.isAdmin)
			.thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
	}
}
