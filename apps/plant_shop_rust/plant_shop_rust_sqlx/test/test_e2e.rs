// ======================================================
// 🧪 Tests End-to-End — Rust clone du test_complet.js
// ======================================================

use chrono::Utc;
use reqwest::blocking::{Client, Response};
use reqwest::header::{HeaderMap, HeaderValue, CONTENT_TYPE, COOKIE};
use serde_json::{json, Value};
use std::{collections::HashMap, time::Duration};

// ------------------------------------------------------
// ⚙️ Contexte principal (client HTTP, cookies, base_url)
// ------------------------------------------------------
struct TestContext {
    client: Client,
    base_url: String,
    cookies: HashMap<String, String>,
    timestamp: String,
}

impl TestContext {
    fn new() -> Self {
        Self {
            client: Client::builder()
                .redirect(reqwest::redirect::Policy::none())
                .timeout(Duration::from_secs(10))
                .build()
                .unwrap(),
            base_url: "http://localhost:4100/api".to_string(),
            cookies: HashMap::new(),
            timestamp: Utc::now().format("%Y%m%d%H%M%S").to_string(),
        }
    }

    // --- Gestion des cookies ---
    fn cookie(&self, who: &str) -> String {
        self.cookies.get(who).cloned().unwrap_or_default()
    }
    fn set_cookie(&mut self, who: &str, cookie: String) {
        self.cookies.insert(who.to_string(), cookie);
    }

    // --- Requête générique ---
    fn request(
        &mut self,
        method: &str,
        path: &str,
        status: u16,
        body: Option<Value>,
        who: &str,
    ) -> Value {
        let url = format!("{}{}", self.base_url, path);
        let mut headers = HeaderMap::new();
        headers.insert(CONTENT_TYPE, HeaderValue::from_static("application/json"));
        if !self.cookie(who).is_empty() {
            headers.insert(COOKIE, HeaderValue::from_str(&self.cookie(who)).unwrap());
        }
        let builder = match method {
            "GET" => self.client.get(&url),
            "POST" => self.client.post(&url),
            "PATCH" => self.client.patch(&url),
            "DELETE" => self.client.delete(&url),
            _ => panic!("Méthode HTTP non supportée"),
        };
        let builder = if let Some(ref b) = body {
            builder.headers(headers.clone()).body(b.to_string())
        } else {
            builder.headers(headers.clone())
        };
        let resp: Response = builder.send().unwrap();
        let code = resp.status().as_u16();
        if let Some(set_cookie) = resp.headers().get("set-cookie") {
            let cookie_val = set_cookie.to_str().unwrap().split(';').next().unwrap();
            self.set_cookie(who, cookie_val.to_string());
        }

        // --- Affichage style test_complet.js ---
        println!(
            "{} {} [{}]",
            if code == status { "✅" } else { "❌" },
            format!("{:<6} {}", method, path),
            code
        );

        if code != status {
            let txt = resp.text().unwrap_or_default();
            panic!(
                "API {} {} → {} (attendu {})\n{}",
                method, path, code, status, txt
            );
        }
        resp.json().unwrap_or(json!({}))
    }

    // --- Actions Auth ---
    fn login(&mut self, email: &str, password: &str, who: &str) {
        let _ = self.request(
            "POST",
            "/auth/login",
            201,
            Some(json!({"email":email,"password":password})),
            who,
        );
    }
    fn register(&mut self, name: &str, email: &str, password: &str, who: &str) {
        let _ = self.request(
            "POST",
            "/auth/register",
            201,
            Some(json!({"name":name,"email":email,"password":password})),
            who,
        );
    }

    // --- Assertions ---
    fn assert_eq(val: &Value, key: &str, expected: &Value) {
        let actual = val
            .get(key)
            .unwrap_or_else(|| panic!("Objet vide – clé {} recherchée", key));
        let ok = actual == expected;
        println!(
            "{}   ↳ {}={} (attendu {:?})",
            if ok { "✅" } else { "❌" },
            key,
            actual,
            expected
        );
        assert_eq!(
            actual, expected,
            "Clé {} attendue {:?}, reçu {:?}",
            key, expected, actual
        );
    }
    fn assert_num(val: &Value, key: &str) {
        let obj = val
            .get(key)
            .unwrap_or_else(|| panic!("Objet vide – clé {} recherchée", key));
        let s = obj.to_string();
        assert!(
            s.chars().all(|c| c.is_ascii_digit()),
            "Clé {} n'est pas numérique: {}",
            key,
            s
        );
    }
}

// ------------------------------------------------------
// 🚀 Exécution principale
// ------------------------------------------------------
fn main() {
    use rand::{distributions::Alphanumeric, Rng};
    let mut ctx = TestContext::new();
    let admin_email = "admin1@planteshop.com";
    let admin_password = "password";
    let random_tag: String = rand::thread_rng()
        .sample_iter(&Alphanumeric)
        .take(4)
        .map(char::from)
        .collect();

    let user_email = format!(
        "utilisateur_de_test_{}_{}@example.com",
        ctx.timestamp, random_tag
    );

    let user_password = "pass123";

    println!("🧪 Démarrage des tests: {}\n", ctx.base_url);

    ctx.login(admin_email, admin_password, "admin");
    ctx.register("User", &user_email, user_password, "user");
    ctx.login(&user_email, user_password, "user");

    test_plants(&mut ctx);
    test_users(&mut ctx);
    test_orders(&mut ctx);
    test_user_profile(&mut ctx, &user_email);
    test_auth_roles(&mut ctx);
    test_admin_plants(&mut ctx);
    test_admin_users(&mut ctx);
    test_auth_me(&mut ctx);

    println!("\n🎉 Tous les tests ont réussi!");
}

// ------------------------------------------------------
// 🧪 Modules de test
// ------------------------------------------------------

fn test_plants(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: PLANTS (admin)");
    let plant_data = json!({"name":format!("Test Plant"),"price":10,"stock":5});
    let plant = ctx.request(
        "POST",
        "/admin/plants",
        201,
        Some(plant_data.clone()),
        "admin",
    );
    TestContext::assert_num(&plant, "id");
    let id = plant["id"].as_u64().unwrap();
    let get = ctx.request("GET", &format!("/plants/{}", id), 200, None, "admin");
    TestContext::assert_eq(&get, "name", &plant_data["name"]);
    ctx.request(
        "PATCH",
        &format!("/admin/plants/{}", id),
        200,
        Some(json!({"price":15})),
        "admin",
    );
    let check = ctx.request("GET", &format!("/plants/{}", id), 200, None, "admin");
    TestContext::assert_eq(&check, "price", &json!(15));
    ctx.request(
        "DELETE",
        &format!("/admin/plants/{}", id),
        200,
        None,
        "admin",
    );
}

fn test_users(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: USERS (admin)");
    let email = format!("utilisateur_test_{}@example.com", ctx.timestamp);
    let user = ctx.request(
        "POST",
        "/users",
        201,
        Some(json!({"email":email,"name":"Utilisateur de test","password":"pass123"})),
        "admin",
    );
    let id = user["id"].as_u64().unwrap();
    ctx.request(
        "PATCH",
        &format!("/users/{}", id),
        200,
        Some(json!({"name":"Tester Update"})),
        "admin",
    );
    let get = ctx.request("GET", &format!("/users/{}", id), 200, None, "admin");
    TestContext::assert_eq(&get, "name", &json!("Tester Update"));
    ctx.request("DELETE", &format!("/users/{}", id), 200, None, "admin");
}

fn test_orders(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: ORDERS & ORDER ITEMS");
    let plant_data =
        json!({"name":format!("Plante_de_test_{}",ctx.timestamp),"price":10,"stock":5});
    let plant = ctx.request(
        "POST",
        "/admin/plants",
        201,
        Some(plant_data.clone()),
        "admin",
    );
    let pid = plant["id"].as_u64().unwrap();
    TestContext::assert_num(&plant, "id");

    let order = ctx.request(
        "POST",
        "/orders",
        201,
        Some(json!({"items":[{"plantId":pid,"quantity":2}]})),
        "user",
    );
    let oid = order["id"].as_u64().unwrap();
    TestContext::assert_num(&order, "id");

    ctx.request(
        "PATCH",
        &format!("/orders/{}", oid),
        200,
        Some(json!({"status":"shipped"})),
        "admin",
    );

    let list = ctx.request("GET", "/orders", 200, None, "user");
    let arr = list.as_array().unwrap();
    let found = arr
        .iter()
        .find(|o| o["id"] == oid)
        .expect("Commande absente");
    TestContext::assert_eq(found, "status", &json!("shipped"));
    if !found.get("orderItems").is_some() || found["orderItems"].as_array().unwrap().is_empty() {
        panic!("Items absents dans la commande");
    }
    TestContext::assert_eq(
        &found["orderItems"][0]["plant"],
        "name",
        &plant_data["name"],
    );

    ctx.request("DELETE", &format!("/orders/{}", oid), 200, None, "admin");
    ctx.request(
        "DELETE",
        &format!("/admin/plants/{}", pid),
        200,
        None,
        "admin",
    );
}

fn test_user_profile(ctx: &mut TestContext, email: &str) {
    println!("\n📌 TEST MODULE: USER PROFILE (user)");
    let users = ctx.request("GET", "/users", 200, None, "admin");
    let arr = users.as_array().unwrap();
    let u = arr.iter().find(|x| x["email"] == email).unwrap();
    let uid = u["id"].as_u64().unwrap();
    let profile = ctx.request("GET", &format!("/users/{}", uid), 200, None, "user");
    TestContext::assert_eq(&profile, "id", &json!(uid));
    let new_name = format!("Utilisateur_de_test_{}", ctx.timestamp);
    ctx.request(
        "PATCH",
        &format!("/users/{}", uid),
        200,
        Some(json!({"name":new_name.clone()})),
        "user",
    );
    let updated = ctx.request("GET", &format!("/users/{}", uid), 200, None, "user");
    TestContext::assert_eq(&updated, "name", &json!(new_name));
    ctx.request(
        "PATCH",
        &format!("/users/{}", uid),
        200,
        Some(json!({"admin":true})),
        "user",
    );
    let check = ctx.request("GET", &format!("/users/{}", uid), 200, None, "admin");
    TestContext::assert_eq(&check, "admin", &json!(false));
}

fn test_auth_roles(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: ROLES");
    ctx.request(
        "POST",
        "/admin/plants",
        403,
        Some(json!({"name":"Bad","price":1,"stock":1})),
        "user",
    );
    let plant = ctx.request(
        "POST",
        "/admin/plants",
        201,
        Some(json!({"name":"Good","price":1,"stock":1})),
        "admin",
    );
    let pid = plant["id"].as_u64().unwrap();
    ctx.request(
        "DELETE",
        &format!("/admin/plants/{}", pid),
        200,
        None,
        "admin",
    );
    ctx.request("GET", "/users", 403, None, "user");
}

fn test_admin_plants(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: ADMIN PLANTS");
    let plantes = ctx.request("GET", "/admin/plants", 200, None, "admin");
    let arr = plantes.as_array().unwrap();
    println!("   ↳ {} plantes récupérées", arr.len());
    let d = json!({"name":format!("Plante_admin_{}",ctx.timestamp),"price":99,"stock":12});
    let p = ctx.request("POST", "/admin/plants", 201, Some(d.clone()), "admin");
    let id = p["id"].as_u64().unwrap();
    ctx.request(
        "PATCH",
        &format!("/admin/plants/{}", id),
        200,
        Some(json!({"price":123})),
        "admin",
    );
    ctx.request(
        "DELETE",
        &format!("/admin/plants/{}", id),
        200,
        None,
        "admin",
    );
}

fn test_admin_users(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: ADMIN USERS");
    let email = format!("admin_temp_{}@example.com", ctx.timestamp);
    let name = format!("Admin Temporaire {}", ctx.timestamp);
    let temp = ctx.request(
        "POST",
        "/users",
        201,
        Some(json!({"email":email,"name":name,"password":"password","admin":true})),
        "admin",
    );
    let id = temp["id"].as_u64().unwrap();

    let list = ctx.request("GET", "/admin/users", 200, None, "admin");
    let arr = list.as_array().unwrap();
    let cible = arr
        .iter()
        .find(|a| a["email"] == email)
        .expect("L’admin temporaire n’a pas été trouvé dans la liste !");
    TestContext::assert_eq(&cible, "name", &json!(name));

    let nouveau_nom = format!("Admin_temp_modifié_{}", ctx.timestamp);
    ctx.request(
        "PATCH",
        &format!("/users/{}", id),
        200,
        Some(json!({"name": nouveau_nom.clone()})),
        "admin",
    );
    let user_get = ctx.request("GET", &format!("/users/{}", id), 200, None, "admin");
    TestContext::assert_eq(&user_get, "name", &json!(nouveau_nom));

    ctx.request("DELETE", &format!("/users/{}", id), 200, None, "admin");
}

fn test_auth_me(ctx: &mut TestContext) {
    println!("\n📌 TEST MODULE: AUTH /me");
    let me = ctx.request("GET", "/auth/me", 200, None, "user");
    let mail = me["email"].as_str().unwrap_or("?");
    let nom = me["name"].as_str().unwrap_or("??");
    TestContext::assert_eq(&me, "email", &json!(mail));
    TestContext::assert_eq(&me, "name", &json!(nom));
    println!("   ↳ Utilisateur connecté: {} ({})", mail, nom);
}
