package Tests;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Test end-to-end minimal – inspiré du test_e2e.cpp
 * :contentReference[oaicite:0]{index=0}.
 * Java 1.6, aucune lambda, pas de framework.
 */
public class test {

	// ---------- Config ----------
	private static final String SERVER_PORT =
    System.getenv("SERVER_ADDRESS") != null ? System.getenv("SERVER_ADDRESS") : "4100";

	private static final String BASE_URL = "http://localhost:" + SERVER_PORT + "/api";
	private static final String ADMIN_EMAIL = "admin1@planteshop.com";
	private static final String ADMIN_PWD = "password";

	// ---------- Cookie store ----------
	private final Map<String, String> cookiePerUser = new HashMap<String, String>();

	// ---------- Utilitaires ----------
	private static String timestamp() {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	}

	private static String randTag(int len) {
		String alpha = "abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb = new StringBuilder();
		Random r = new Random();
		for (int i = 0; i < len; i++)
			sb.append(alpha.charAt(r.nextInt(alpha.length())));
		return sb.toString();
	}

	// ---------- HTTP générique ----------
	private JSONObject request(
			String method, String path, int expected,
			JSONObject body, String who) throws Exception {

		URL url = new URL(BASE_URL + path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setRequestProperty("Content-Type", "application/json");

		// Cookie sortant
		String ck = cookiePerUser.get(who);
		if (ck != null)
			conn.setRequestProperty("Cookie", ck);

		// Body
		if (body != null) {
			conn.setDoOutput(true);
			OutputStream os = conn.getOutputStream();
			os.write(body.toString().getBytes("UTF-8"));
			os.close();
		}

		int code = conn.getResponseCode();

		// Cookie entrant
		String setCookie = conn.getHeaderField("Set-Cookie");
		if (setCookie != null) {
			String shortCk = setCookie.split(";", 2)[0];
			cookiePerUser.put(who, shortCk);
		}

		// Log style test_original
		System.out.printf("%s %-6s %s [%d]%n",
				code == expected ? "✅" : "❌",
				method, path, code);

		if (code != expected) {
			InputStream es = conn.getErrorStream();
			if (es == null)
				es = conn.getInputStream();
			String err = streamToString(es);
			throw new RuntimeException(
					"API " + method + " " + path + " → " + code + " (attendu " + expected + ")\n" + err);
		}

		// Corps JSON si présent
		String ct = conn.getHeaderField("Content-Type");
		boolean isJson = ct != null && ct.startsWith("application/json");
		if (isJson) {
			String txt = streamToString(conn.getInputStream());
			if (txt.trim().isEmpty())
				return new JSONObject();
			return new JSONObject(txt);
		}
		return new JSONObject();
	}

	private static String streamToString(InputStream is) throws IOException {
		if (is == null)
			return "";
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null)
			sb.append(line);
		return sb.toString();
	}

	// ---------- Actions Auth ----------
	private void login(String email, String pwd, String who) throws Exception {
		JSONObject creds = new JSONObject();
		creds.put("email", email).put("password", pwd);
		request("POST", "/auth/login", 201, creds, who);
	}

	private void register(String name, String email, String pwd, String who) throws Exception {
		JSONObject u = new JSONObject();
		u.put("name", name).put("email", email).put("password", pwd);
		request("POST", "/auth/register", 201, u, who);
	}

	// ---------- Assertions ----------
	private static void assertEq(JSONObject obj, String key, Object exp) {
		Object act = obj.opt(key);
		boolean ok = (act == null) ? exp == null : act.equals(exp);
		System.out.printf("%s   ↳ %s=%s (attendu %s)%n",
				ok ? "✅" : "❌", key, act, exp);
		if (!ok)
			throw new RuntimeException("Assertion échouée sur " + key);
	}

	private static void assertNum(JSONObject obj, String key) {
		Object v = obj.opt(key);
		if (!(v instanceof Number))
			throw new RuntimeException("Clé " + key + " non numérique");
	}

	// ---------- Modules de test ----------
	private void testPlants() throws Exception {
		System.out.println("\n📌 TEST MODULE: PLANTS (admin)");
		JSONObject data = new JSONObject()
				.put("name", "Test Plant")
				.put("price", 10)
				.put("stock", 5);
		JSONObject plant = request("POST", "/admin/plants", 201, data, "admin");
		assertNum(plant, "id");
		int id = plant.getInt("id");

		JSONObject get = request("GET", "/plants/" + id, 200, null, "admin");
		assertEq(get, "name", data.get("name"));

		request("PATCH", "/admin/plants/" + id,
				200, new JSONObject().put("price", 15), "admin");

		JSONObject chk = request("GET", "/plants/" + id, 200, null, "admin");
		assertEq(chk, "price", 15);

		request("DELETE", "/admin/plants/" + id, 200, null, "admin");
	}

	private void testUsers() throws Exception {
		System.out.println("\n📌 TEST MODULE: USERS (admin)");
		String email = "utilisateur_test_" + timestamp() + "@example.com";
		JSONObject u = new JSONObject()
				.put("email", email)
				.put("name", "Utilisateur de test")
				.put("password", "pass123");
		JSONObject user = request("POST", "/users", 201, u, "admin");
		int id = user.getInt("id");

		request("PATCH", "/users/" + id,
				200, new JSONObject().put("name", "Tester Update"), "admin");

		JSONObject chk = request("GET", "/users/" + id, 200, null, "admin");
		assertEq(chk, "name", "Tester Update");

		request("DELETE", "/users/" + id, 200, null, "admin");
	}

	// ---------- Main ----------
	public static void main(String[] args) throws Exception {

		// Attendre le serveur (5 s max)
		long t0 = System.currentTimeMillis();
		while (true) {
			try {
				Socket s = new Socket();
				s.connect(new InetSocketAddress("127.0.0.1", SERVER_PORT), 300);
				s.close();
				break;
			} catch (IOException e) {
				if (System.currentTimeMillis() - t0 > 5000) {
					System.err.println("❌ Serveur injoignable http://localhost:" + SERVER_PORT);
					System.exit(2);
				}
				Thread.sleep(200);
			}
		}

		test ctx = new test();

		// Préparation comptes
		ctx.login(ADMIN_EMAIL, ADMIN_PWD, "admin");

		String rnd = randTag(4);
		String userMail = "utilisateur_de_test_" + timestamp() + "_" + rnd + "@example.com";
		ctx.register("User", userMail, "pass123", "user");
		ctx.login(userMail, "pass123", "user");

		// Exécution modules
		ctx.testPlants();
		ctx.testUsers();

		System.out.println("\n🎉 Tous les tests Java ont réussi !");
	}
}
