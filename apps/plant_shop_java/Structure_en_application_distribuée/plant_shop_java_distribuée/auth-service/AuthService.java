import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import util.PasswordUtil;
import util.Request;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service d'authentification pour la gestion des utilisateurs et sessions.
 */
public final class AuthService {

    private static Connection db;
    private static HttpServer server;

    /**
	 * Point d'entrée du service d'authentification.
	 * @param args Arguments de ligne de commande
	 */
	public static void main(String[] args) throws Exception {
        Map<String, String> cfg = loadEnv();

        int port = Integer.parseInt(cfg.getOrDefault("AUTH_SERVICE_PORT", "6101"));
        String url = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_microservices");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");

        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis (config/.env).");
        }

        db = DriverManager.getConnection(url, user, pass);

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/auth", new AuthRoutes(db));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.printf("🔐 AuthService démarré sur http://localhost:%d/auth%n", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                server.stop(0);
            }
            if (db != null) {
                try {
                    db.close();
                } catch (SQLException ignored) {}
            }
        }));
    }

    /**
	 * Charge les variables d'environnement depuis les fichiers .env.
	 * @return Map des variables d'environnement
	 */
	private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(Path.of("../config/.env"), values);
        readEnv(Path.of(".env"), values);
        return values;
    }

    /**
	 * Lit un fichier .env et ajoute les valeurs à la map.
	 * @param path Chemin du fichier .env
	 * @param values Map où stocker les valeurs
	 */
	private static void readEnv(Path path, Map<String, String> values) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        }
    }
}

/**
 * Routes HTTP pour le service d'authentification.
 */
final class AuthRoutes implements HttpHandler {
    private final AuthController auth;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	AuthRoutes(Connection db) {
        this.auth = new AuthController(db);
    }

    /**
     * Traite une requête HTTP entrante.
     * @param exchange L'échange HTTP
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        auth.handle(exchange);
    }
}

/**
 * Contrôleur de base avec méthodes utilitaires communes.
 */
abstract class BaseController {
    protected final Connection db;
    private final UserRepository userRepo;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	BaseController(Connection db) {
        this.db = db;
        this.userRepo = new UserRepository(db);
    }

    /**
	 * Parse le corps JSON de la requête.
	 * @param ex L'échange HTTP
	 * @return L'objet JSON parsé
	 */
	protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return new JSONObject();
        }
        return new JSONObject(body);
    }

    /**
	 * Envoie une réponse JSON.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param body Le corps JSON
	 */
	protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse JSON sous forme de chaîne.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param jsonBody Le corps JSON en chaîne
	 */
	protected void sendJson(HttpExchange ex, int code, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse vide.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 */
	protected void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    /**
	 * Récupère l'utilisateur authentifié depuis la session.
	 * @param ex L'échange HTTP
	 * @return L'utilisateur ou null
	 */
	protected User getAuthenticatedUser(HttpExchange ex) throws SQLException {
        String sessionId = Request.extractSessionId(ex);
        if (sessionId == null) {
            return null;
        }
        Integer userId = AuthController.sessions.get(sessionId);
        if (userId == null) {
            return null;
        }
        return userRepo.find(userId);
    }

    /**
	 * Gère une exception et envoie une erreur 500.
	 * @param ex L'échange HTTP
	 * @param e L'exception à gérer
	 */
	protected void handleException(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        JSONObject error = new JSONObject().put("error", "Erreur interne: " + e.getMessage());
        sendJson(ex, 500, error);
    }
}

/**
 * Contrôleur pour les opérations d'authentification.
 */
final class AuthController extends BaseController {

    static final Map<String, Integer> sessions = new ConcurrentHashMap<>();
    private final UserRepository userRepo;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	AuthController(Connection db) {
        super(db);
        this.userRepo = new UserRepository(db);
    }

    /**
	 * Route les requêtes vers les bonnes méthodes.
	 * @param ex L'échange HTTP
	 */
	public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath().substring("/auth".length());
        if (path.isEmpty()) {
            path = "/";
        }
        String method = ex.getRequestMethod();

        try {
            if ("POST".equals(method) && "/register".equals(path)) {
                register(ex);
                return;
            }
            if ("POST".equals(method) && "/login".equals(path)) {
                login(ex);
                return;
            }
            if ("GET".equals(method) && "/me".equals(path)) {
                me(ex);
                return;
            }
            if ("GET".equals(method) && "/_session".equals(path)) {
                sessionInfo(ex);
                return;
            }
            if ("/logout".equals(path)) {
                logout(ex);
                return;
            }
            sendJson(ex, 404, new JSONObject().put("error", "Route non trouvée"));
        } catch (Exception e) {
            handleException(ex, e);
        }
    }

    /**
	 * Enregistre un nouvel utilisateur.
	 * @param ex L'échange HTTP
	 */
	private void register(HttpExchange ex) throws Exception {
        JSONObject body = parseJson(ex);
        String name = body.optString("name", null);
        String email = body.optString("email", null);
        String password = body.optString("password", null);

        if (name == null || email == null || password == null) {
            sendJson(ex, 400, "{\"error\":\"Les champs name, email et password sont requis\"}");
            return;
        }

        if (userRepo.findByEmailWithPassword(email) != null) {
            sendJson(ex, 409, "{\"error\":\"Cet email est déjà utilisé.\"}");
            return;
        }

        User newUser = new User(name, email, PasswordUtil.hashPassword(password), false);
        int id = userRepo.create(newUser);
        JSONObject response = new JSONObject().put("id", id).put("message", "Utilisateur créé");
        sendJson(ex, 201, response);
    }

    /**
	 * Connecte un utilisateur.
	 * @param ex L'échange HTTP
	 */
	private void login(HttpExchange ex) throws Exception {
        JSONObject body = parseJson(ex);
        String email = body.optString("email", null);
        String password = body.optString("password", null);

        if (email == null || password == null) {
            sendJson(ex, 400, "{\"error\":\"email et password sont requis\"}");
            return;
        }

        User user = userRepo.findByEmailWithPassword(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.passwordHash)) {
            sendJson(ex, 401, "{\"error\":\"Identifiants invalides\"}");
            return;
        }

        String sessionId = java.util.UUID.randomUUID().toString();
        sessions.put(sessionId, user.id);

        ex.getResponseHeaders().add("Set-Cookie",
            "session_id=" + sessionId + "; HttpOnly; Path=/; Max-Age=3600; SameSite=Lax");

        JSONObject payload = new JSONObject()
            .put("id", user.id)
            .put("name", user.name)
            .put("email", user.email)
            .put("admin", user.isAdmin);
        sendJson(ex, 201, payload);
    }

    /**
	 * Déconnecte l'utilisateur.
	 * @param ex L'échange HTTP
	 */
	private void logout(HttpExchange ex) throws Exception {
        String sessionId = Request.extractSessionId(ex);
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        ex.getResponseHeaders().add("Set-Cookie",
            "session_id=deleted; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        sendEmpty(ex, 204);
    }

    /**
	 * Retourne les infos de l'utilisateur connecté.
	 * @param ex L'échange HTTP
	 */
	private void me(HttpExchange ex) throws Exception {
        User user = getAuthenticatedUser(ex);
        if (user == null) {
            sendJson(ex, 401, "{\"error\":\"Non authentifié\"}");
            return;
        }
        sendJson(ex, 200, user.toJson());
    }

    /**
	 * Retourne les infos de session pour la gateway.
	 * @param ex L'échange HTTP
	 */
	private void sessionInfo(HttpExchange ex) throws Exception {
        User user = getAuthenticatedUser(ex);
        if (user == null) {
            sendJson(ex, 401, "{\"error\":\"Session invalide\"}");
            return;
        }
        sendJson(ex, 200, user.toJson());
    }
}

/**
 * Modèle représentant un utilisateur.
 */
final class User {
    int id;
    String name;
    String email;
    String passwordHash;
    boolean isAdmin;
    Instant createdAt;

    /**
	 * Constructeur complet.
	 * @param id Identifiant
	 * @param name Nom
	 * @param email Email
	 * @param passwordHash Hash du mot de passe
	 * @param isAdmin Si administrateur
	 * @param createdAt Date de création
	 */
	User(int id, String name, String email, String passwordHash, boolean isAdmin, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }

    /**
	 * Constructeur pour création.
	 * @param name Nom
	 * @param email Email
	 * @param passwordHash Hash du mot de passe
	 * @param isAdmin Si administrateur
	 */
	User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    /**
	 * Convertit l'utilisateur en JSON.
	 * @return L'objet JSON
	 */
	JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("email", email);
        json.put("admin", isAdmin);
        if (createdAt != null) {
            json.put("createdAt", createdAt.toString());
        }
        return json;
    }
}

/**
 * Repository pour les opérations sur les utilisateurs.
 */
final class UserRepository {
    private final Connection db;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	UserRepository(Connection db) {
        this.db = db;
    }

    /**
	 * Trouve un utilisateur par son identifiant.
	 * @param id L'identifiant de l'utilisateur
	 * @return L'utilisateur ou null
	 */
	User find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs, true);
                }
            }
        }
        return null;
    }

    /**
	 * Trouve un utilisateur par email avec son mot de passe.
	 * @param email L'email de l'utilisateur
	 * @return L'utilisateur ou null
	 */
	User findByEmailWithPassword(String email) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs, true);
                }
            }
        }
        return null;
    }

    /**
	 * Crée un nouvel utilisateur.
	 * @param u L'utilisateur à créer
	 * @return L'identifiant généré
	 */
	int create(User u) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO users(name, email, password_hash, is_admin) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.name);
            ps.setString(2, u.email);
            ps.setString(3, u.passwordHash);
            ps.setBoolean(4, u.isAdmin);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
	 * Mappe un ResultSet vers un objet User.
	 * @param rs Le ResultSet
	 * @param includePassword Si inclure le mot de passe
	 * @return L'utilisateur mappé
	 */
	private User map(ResultSet rs, boolean includePassword) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            includePassword ? rs.getString("password_hash") : null,
            rs.getBoolean("is_admin"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }
}
