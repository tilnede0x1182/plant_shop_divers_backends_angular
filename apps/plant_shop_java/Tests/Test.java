package Tests;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.JSONObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Test end-to-end Java 1.6 – aligné sur le scénario JS complet.
 * Aucune dépendance hors org.json.
 */
public final class Test {

	/* -------- .env -------- */
	private static Map<String,String> env() throws IOException {
		Map<String,String> m = new HashMap<String,String>();
		BufferedReader br = new BufferedReader(new FileReader(".env"));
		String l; while ((l = br.readLine()) != null) {
			int i = l.indexOf('=');
			if (i > 0) m.put(l.substring(0,i).trim(), l.substring(i+1).trim());
		}
		br.close(); return m;
	}

	/* -------- Config -------- */
	private static final Map<String,String> CFG;
	static { try { CFG = env(); } catch (Exception e) { throw new RuntimeException(e); } }
	private static final String PORT  = CFG.get("SERVER_ADDRESS")!=null ? CFG.get("SERVER_ADDRESS") : "4100";
	private static final String BASE  = "http://localhost:"+PORT+"/api";
	private static final String ADMIN_EMAIL = "admin1@planteshop.com";
	private static final String ADMIN_PWD   = "password";

	/* -------- Cookies -------- */
	private final Map<String,String> cookie = new HashMap<String,String>();

	/* -------- Utilitaires -------- */
	private static String ts(){ return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()); }
	private static String rand(int n){
		String a="abcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder sb=new StringBuilder(); Random r=new Random();
		for(int i=0;i<n;i++) sb.append(a.charAt(r.nextInt(a.length())));
		return sb.toString();
	}

	private JSONObject call(String m, String p, int exp, JSONObject body, String who) throws Exception {
		HttpClient client = HttpClient.newBuilder().build();
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(BASE + p))
			.method(
				m,
				body == null
					? HttpRequest.BodyPublishers.noBody()
					: HttpRequest.BodyPublishers.ofString(body.toString())
			)
			.header("Content-Type", "application/json");
		if (cookie.get(who) != null)
			builder.header("Cookie", cookie.get(who));

		HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
		int code = response.statusCode();
		String set = response.headers().firstValue("Set-Cookie").orElse(null);
		if (set != null)
			cookie.put(who, set.split(";", 2)[0]);

		System.out.printf("%s %-6s %s [%d]%n", code == exp ? "✅" : "❌", m, p, code);

		if (code != exp)
			throw new RuntimeException(response.body());
		if (response.headers().firstValue("Content-Type").orElse("").startsWith("application/json")) {
			String txt = response.body();
			return txt.trim().isEmpty() ? new JSONObject() : new JSONObject(txt);
		}
		return new JSONObject();
	}

	private static String stream(InputStream is)throws IOException{
		if(is==null) return "";
		BufferedReader br=new BufferedReader(new InputStreamReader(is,"UTF-8"));
		StringBuilder sb=new StringBuilder(); String l;
		while((l=br.readLine())!=null) sb.append(l);
		return sb.toString();
	}

	/** Autorise la méthode PATCH sur HttpURLConnection pour Java <= 8 */
	private static void allowHttpPatch() {
		try {
			java.lang.reflect.Field f = java.net.HttpURLConnection.class.getDeclaredField("methods");
			f.setAccessible(true);
			String[] old = (String[]) f.get(null);
			for (String s : old) if ("PATCH".equals(s)) return;
			java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(old));
			list.add("PATCH");
			java.lang.reflect.Field modifiers = java.lang.reflect.Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
			f.set(null, list.toArray(new String[0]));
		} catch (Exception ignore) { }
	}

	/* -------- Auth -------- */

	private void login(String mail,String pw,String who) throws Exception {
		JSONObject j = new JSONObject().put("email", mail).put("password", pw);
		call("POST", "/auth/login", 201, j, who);	// statut aligné sur le test JS
	}
	private void register(String name,String mail,String pw,String who)throws Exception{
		JSONObject j=new JSONObject().put("name",name).put("email",mail).put("password",pw);
		call("POST","/auth/register",201,j,who);
	}

	/* -------- Assertions simples -------- */
	private static void eq(JSONObject o,String k,Object e){
		Object a=o.opt(k);
		if(e==null ? a!=null : !e.equals(a))
			throw new RuntimeException("Mismatch "+k+"="+a+" attendu "+e);
	}
	private static void num(JSONObject o,String k){
		if(!(o.opt(k) instanceof Number))
			throw new RuntimeException("Key "+k+" not numeric");
	}

	/* -------- Tests CRUD plantes publiques -------- */
	private void testPlants() throws Exception{
			System.out.println("\n📌 PLANTS");
			JSONObject req = new JSONObject()
					.put("name","Plante-test")
					.put("price",9.9)
					.put("stock",3);

			/* création (end-point admin) */
			JSONObject pl = call("POST","/admin/plants",201,req,"admin");
			num(pl,"id"); int id = pl.getInt("id");

			/* lecture publique */
			JSONObject get = call("GET","/plants/"+id,200,null,"admin");
			eq(get,"name","Plante-test");

			/* mise à jour (admin) */
			call("PATCH","/admin/plants/"+id,200,new JSONObject().put("stock",8),"admin");
			JSONObject chk = call("GET","/plants/"+id,200,null,"admin");
			eq(chk,"stock",8);

			/* suppression (admin) */
			call("DELETE","/admin/plants/"+id,200,null,"admin");
	}

	/* -------- Tests CRUD users publics -------- */
	private void testUsers() throws Exception{
		System.out.println("\n📌 USERS");
		String mail="u_"+ts()+"_"+rand(4)+"@mail.test";
		JSONObject u=new JSONObject().put("name","User").put("email",mail).put("password","pw");
		JSONObject r=call("POST","/users",201,u,"admin"); int id=r.getInt("id");

		call("PATCH","/users/"+id,200,new JSONObject().put("name","UserX"),"admin");
		JSONObject chk=call("GET","/users/"+id,200,null,"admin");
		eq(chk,"name","UserX");

		call("DELETE","/users/"+id,200,null,"admin");
	}

	/* -------- Tests rôles/permissions -------- */
	private void testAuthRoles() throws Exception{
		System.out.println("\n📌 ROLES");
		JSONObject bad=new JSONObject().put("name","Bad").put("price",1).put("stock",1);
		call("POST","/admin/plants",403,bad,"client");          // user bloqué

		JSONObject good=new JSONObject().put("name","Good").put("price",1).put("stock",1);
		JSONObject ok=call("POST","/admin/plants",201,good,"admin");
		int pid=ok.getInt("id");
		call("DELETE","/admin/plants/"+pid,200,null,"admin");   // nettoyage

		call("GET","/users",403,null,"client");                 // liste interdite
	}

	/* -------- Tests CRUD plantes admin -------- */
	private void testAdminPlants() throws Exception{
		System.out.println("\n📌 ADMIN PLANTS");
		JSONObject d=new JSONObject().put("name","Plante_admin_"+ts())
		                             .put("price",99).put("stock",12);
		JSONObject pl=call("POST","/admin/plants",201,d,"admin");
		int id=pl.getInt("id");
		call("PATCH","/admin/plants/"+id,200,new JSONObject().put("price",123),"admin");
		call("DELETE","/admin/plants/"+id,200,null,"admin");
	}

	/* -------- Tests CRUD users admin -------- */
	private void testAdminUsers() throws Exception{
		System.out.println("\n📌 ADMIN USERS");
		String mail="admin_temp_"+ts()+"@example.com";
		JSONObject tmp=new JSONObject().put("name","AdminTemp").put("email",mail)
		                               .put("password","password").put("admin",true);
		JSONObject cr=call("POST","/users",201,tmp,"admin");
		int id=cr.getInt("id");
		call("DELETE","/users/"+id,200,null,"admin");
	}

	/* -------- Main -------- */
	public static void main(String[] args) throws Exception {
		Test t=new Test();

		try { t.login(ADMIN_EMAIL, ADMIN_PWD, "admin"); }
		catch(RuntimeException e){
			System.err.println("❌ /auth/login indisponible"); System.exit(1);
		}

		/* user standard */
		String mail="cli_"+ts()+"@mail.test";
		t.register("Client",mail,"pass","client");
		t.login(mail,"pass","client");

		/* suites */
		t.testPlants();
		t.testUsers();
		t.testAuthRoles();
		t.testAdminPlants();
		t.testAdminUsers();

		System.out.println("\n🎉 Tous les tests OK");
	}
}
