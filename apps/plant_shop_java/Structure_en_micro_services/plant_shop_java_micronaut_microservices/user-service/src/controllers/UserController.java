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
import model.UserDTO;
import repository.UserRepository;
import security.Guards;
import util.ApiMapper;
import util.PasswordUtil;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 */
@Controller("/")
public class UserController {

	private final UserRepository repo;

	/**
	 * Construit le contrôleur avec la connexion BDD.
	 * @param db Connexion à la base de données
	 */
	@Inject
	public UserController(Connection db) {
		this.repo = new UserRepository(db);
	}

	/**
	 * Liste tous les utilisateurs (admin).
	 * @param request Requête HTTP
	 * @return Liste des utilisateurs
	 * @throws Exception En cas d'erreur BDD
	 */
	@Get("/admin/users")
	public List<?> listAdmin(HttpRequest<?> request) throws Exception {
		return listImpl(request);
	}

	/**
	 * Alias pour lister les utilisateurs.
	 * @param request Requête HTTP
	 * @return Liste des utilisateurs
	 * @throws Exception En cas d'erreur BDD
	 */
	@Get("/users")
	public List<?> listAlias(HttpRequest<?> request) throws Exception {
		return listImpl(request);
	}

	/**
	 * Affiche un utilisateur par son ID.
	 * @param id Identifiant de l'utilisateur
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	@Get("/users/{id}")
	public HttpResponse<?> show(@PathVariable int id, HttpRequest<?> request) throws Exception {
		UserDTO currentUser = Guards.requireUser(request);
		if (currentUser.id != id && !currentUser.isAdmin) return HttpResponse.status(HttpStatus.FORBIDDEN);
		User user = repo.find(id);
		return user != null ? HttpResponse.ok(ApiMapper.toUser(user)) : HttpResponse.notFound();
	}

	/**
	 * Crée un nouvel utilisateur (admin).
	 * @param userData Données de l'utilisateur
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
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
	 * @param id Identifiant de l'utilisateur
	 * @param updatedData Données mises à jour
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	@Patch("/users/{id}")
	public HttpResponse<?> updateUser(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
		return updateImpl(id, updatedData, request);
	}

	/**
	 * Alias admin pour mise à jour utilisateur.
	 * @param id Identifiant de l'utilisateur
	 * @param updatedData Données mises à jour
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	@Patch("/admin/users/{id}")
	public HttpResponse<?> updateAdminAlias(@PathVariable int id, @Body User updatedData, HttpRequest<?> request) throws Exception {
		return updateImpl(id, updatedData, request);
	}

	/**
	 * Supprime un utilisateur (admin).
	 * @param id Identifiant de l'utilisateur
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	@Delete("/users/{id}")
	public HttpResponse<?> destroyUser(@PathVariable int id, HttpRequest<?> request) throws Exception {
		return destroyImpl(id, request);
	}

	/**
	 * Alias admin pour suppression utilisateur.
	 * @param id Identifiant de l'utilisateur
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	@Delete("/admin/users/{id}")
	public HttpResponse<?> destroyAdminAlias(@PathVariable int id, HttpRequest<?> request) throws Exception {
		return destroyImpl(id, request);
	}

	/**
	 * Implémentation de la liste des utilisateurs.
	 * @param request Requête HTTP
	 * @return Liste des utilisateurs
	 * @throws Exception En cas d'erreur BDD
	 */
	private List<?> listImpl(HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		return repo.list().stream()
			.sorted(userComparator())
			.map(ApiMapper::toUser)
			.collect(Collectors.toList());
	}

	/**
	 * Implémentation de la mise à jour utilisateur.
	 * @param id Identifiant de l'utilisateur
	 * @param updatedData Données mises à jour
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	private HttpResponse<?> updateImpl(int id, User updatedData, HttpRequest<?> request) throws Exception {
		UserDTO currentUser = Guards.requireUser(request);
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
	 * Implémentation de la suppression utilisateur.
	 * @param id Identifiant de l'utilisateur
	 * @param request Requête HTTP
	 * @return Réponse HTTP
	 * @throws Exception En cas d'erreur BDD
	 */
	private HttpResponse<?> destroyImpl(int id, HttpRequest<?> request) throws Exception {
		Guards.requireAdmin(request);
		repo.delete(id);
		return HttpResponse.ok();
	}

	/**
	 * Retourne un comparateur triant les utilisateurs.
	 * @return Comparateur d'utilisateurs
	 */
	private Comparator<User> userComparator() {
		return Comparator.comparing((User u) -> !u.isAdmin)
			.thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
	}
}
