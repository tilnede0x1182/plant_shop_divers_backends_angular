use reqwest;
use serde_json::json;
use std::collections::HashMap;
use std::env;
use std::error::Error;

// Variables globales
struct Config {
    api_base_url: String,
    log_level: String,
    admin_email: String,
    admin_password: String,
}

impl Config {
    fn new() -> Self {
        let api_base_url = env::var("API_BASE_URL").unwrap_or_else(|_| "http://localhost:4100/api".to_string());
        let log_level = "verbose".to_string();
        let admin_email = env::var("ADMIN_EMAIL").unwrap_or_else(|_| "admin1@planteshop.com".to_string());
        let admin_password = env::var("ADMIN_PASSWORD").unwrap_or_else(|_| "password".to_string());

        Self {
            api_base_url,
            log_level,
            admin_email,
            admin_password,
        }
    }
}

struct CookieJars {
    admin: String,
    user: String,
}

impl CookieJars {
    fn new() -> Self {
        Self {
            admin: "".to_string(),
            user: "".to_string(),
        }
    }
}

// Fonctions utilitaires
async fn hit(method: &str, route: &str, expected_status: u16, body: Option<serde_json::Value>, who: &str) -> Result<serde_json::Value, Box<dyn Error>> {
    let config = Config::new();
    let cookie_jars = CookieJars::new();

    let url = format!("{}{}", config.api_base_url, route);
    let label = format!("{} {}", method, route);

    let client = reqwest::Client::new();
    let mut request = client.request(method, &url);

    if let Some(body) = body {
        request = request.json(&body);
    }

    if who == "admin" {
        request = request.header("Cookie", &cookie_jars.admin);
    } else if who == "user" {
        request = request.header("Cookie", &cookie_jars.user);
    }

    let response = request.send().await.map_err(|e| AppError::DatabaseError(e))?
;

    if response.status() != expected_status {
        let text = response.text().await.map_err(|e| AppError::DatabaseError(e))?
;
        return Err(format!("API {} → {} (attendu {})\n{}", label, response.status(), expected_status, text).into());
    }

    let json = response.json().await.map_err(|e| AppError::DatabaseError(e))?
;
    Ok(json)
}

// Assertions
fn assert_eq(obj: &serde_json::Value, key: &str, expected: &str) {
    let actual = obj.get(key).unwrap().as_str().unwrap();
    let ok = actual == expected;

    if !ok {
        panic!("Assertion failed: {} = {}, expected {}", key, actual, expected);
    }
}

// Helpers
async fn login(email: &str, password: &str, who: &str) -> Result<(), Box<dyn Error>> {
    let body = json!({ "email": email, "password": password });
    hit("POST", "/auth/login", 201, Some(body), who).await.map_err(|e| AppError::DatabaseError(e))?
;
    Ok(())
}

async fn register_user(name: &str, email: &str, password: &str, who: &str) -> Result<(), Box<dyn Error>> {
    let body = json!({ "name": name, "email": email, "password": password });
    hit("POST", "/auth/register", 201, Some(body), who).await.map_err(|e| AppError::DatabaseError(e))?
;
    Ok(())
}

async fn find_user_id_by_email(who: &str, email: &str) -> Result<u32, Box<dyn Error>> {
    let users = hit("GET", "/users", 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;
    let user = users.as_array().unwrap().iter().find(|usr| usr.get("email").unwrap().as_str().unwrap() == email).unwrap();
    let user_id = user.get("id").unwrap().as_u64().unwrap() as u32;
    Ok(user_id)
}

// Modules de test
async fn test_plants(who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: PLANTS (admin)");
    let plant_data = json!({ "name": "Test Plant", "price": 10, "stock": 5 });

    let plant_id = hit("POST", "/admin/plants", 201, Some(plant_data), who).await.map_err(|e| AppError::DatabaseError(e))?
.get("id").unwrap().as_u64().unwrap() as u32;

    assert_eq(&hit("GET", &format!("/plants/{}", plant_id), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
, "name", "Test Plant");

    let updated_plant_data = json!({ "price": 15 });
    hit("PATCH", &format!("/admin/plants/{}", plant_id), 200, Some(updated_plant_data), who).await.map_err(|e| AppError::DatabaseError(e))?
;

    assert_eq(&hit("GET", &format!("/plants/{}", plant_id), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
, "price", 15);

    hit("DELETE", &format!("/admin/plants/{}", plant_id), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;

    Ok(())
}

async fn test_users(who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: USERS (admin)");
    let user_data = json!({
        "email": "utilisateur_test@example.com",
        "name": "Utilisateur de test",
        "password": "pass123"
    });

    let user_id = hit("POST", "/users", 201, Some(user_data), who).await.map_err(|e| AppError::DatabaseError(e))?
.get("id").unwrap().as_u64().unwrap() as u32;

    let updated_user_data = json!({ "name": "Tester Update" });
    hit("PATCH", &format!("/users/{}", user_id), 200, Some(updated_user_data), who).await.map_err(|e| AppError::DatabaseError(e))?
;

    assert_eq(&hit("GET", &format!("/users/{}", user_id), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
, "name", "Tester Update");

    hit("DELETE", &format!("/users/{}", user_id), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;

    Ok(())
}

async fn test_orders(admin_who: &str, user_who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: ORDERS & ORDER ITEMS");

    let plant_data = json!({ "name": "Plante_de_test", "price": 10, "stock": 5 });
    let plant_id = hit("POST", "/admin/plants", 201, Some(plant_data), admin_who).await.map_err(|e| AppError::DatabaseError(e))?
.get("id").unwrap().as_u64().unwrap() as u32;

    let order_payload = json!({ "items": [{ "plantId": plant_id, "quantity": 2 }] });
    let order_id = hit("POST", "/orders", 201, Some(order_payload), user_who).await.map_err(|e| AppError::DatabaseError(e))?
.get("id").unwrap().as_u64().unwrap() as u32;

    let updated_order_data = json!({ "status": "shipped" });
    hit("PATCH", &format!("/orders/{}", order_id), 200, Some(updated_order_data), admin_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    let commandes = hit("GET", "/orders", 200, None, user_who).await.map_err(|e| AppError::DatabaseError(e))?
;
    let commande = commandes.as_array().unwrap().iter().find(|o| o.get("id").unwrap().as_u64().unwrap() as u32 == order_id).unwrap();
    assert_eq(commande, "status", "shipped");

    hit("DELETE", &format!("/orders/{}", order_id), 200, None, admin_who).await.map_err(|e| AppError::DatabaseError(e))?
;
    hit("DELETE", &format!("/admin/plants/{}", plant_id), 200, None, admin_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    Ok(())
}

async fn test_user_profile(admin_who: &str, user_who: &str, user_email: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: USER PROFILE (user)");
    let user_id = find_user_id_by_email(admin_who, user_email).await.map_err(|e| AppError::DatabaseError(e))?
;

    assert_eq(&hit("GET", &format!("/users/{}", user_id), 200, None, user_who).await.map_err(|e| AppError::DatabaseError(e))?
, "id", user_id);

    let updated_user_data = json!({ "name": "Utilisateur_de_test" });
    hit("PATCH", &format!("/users/{}", user_id), 200, Some(updated_user_data), user_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    assert_eq(&hit("GET", &format!("/users/{}", user_id), 200, None, user_who).await.map_err(|e| AppError::DatabaseError(e))?
, "name", "Utilisateur_de_test");

    let elevation_data = json!({ "admin": true });
    hit("PATCH", &format!("/users/{}", user_id), 200, Some(elevation_data), user_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    let profil = hit("GET", &format!("/users/{}", user_id), 200, None, admin_who).await.map_err(|e| AppError::DatabaseError(e))?
;
    assert_eq(&profil, "admin", false);

    Ok(())
}

async fn test_auth_roles(admin_who: &str, user_who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: ROLES");

    let bad_plant_data = json!({ "name": "Bad", "price": 1, "stock": 1 });
    hit("POST", "/admin/plants", 403, Some(bad_plant_data), user_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    let good_plant_data = json!({ "name": "Good", "price": 1, "stock": 1 });
    let plant_id = hit("POST", "/admin/plants", 201, Some(good_plant_data), admin_who).await.map_err(|e| AppError::DatabaseError(e))?
.get("id").unwrap().as_u64().unwrap() as u32;
    hit("DELETE", &format!("/admin/plants/{}", plant_id), 200, None, admin_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    hit("GET", "/users", 403, None, user_who).await.map_err(|e| AppError::DatabaseError(e))?
;

    Ok(())
}

async fn test_admin_plants(who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: ADMIN PLANTS");
    let plantes = hit("GET", "/admin/plants", 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;
    println!("   ↳ {} plantes récupérées", plantes.as_array().unwrap().len());

    let plant_data = json!({ "name": "Plante_admin_de_test", "price": 99, "stock": 12 });
    let plant_id = hit("POST", "/admin/plants", 201, Some(plant_data), who).await.map_err(|e| AppError::DatabaseError(e))?
.get("id").unwrap().as_u64().unwrap() as u32;

    let updated_plant_data = json!({ "price": 123 });
    hit("PATCH", &format!("/admin/plants/{}", plant_id), 200, Some(updated_plant_data), who).await.map_err(|e| AppError::DatabaseError(e))?
;

    hit("DELETE", &format!("/admin/plants/{}", plant_id), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;

    Ok(())
}

async fn test_admin_users(who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: ADMIN USERS");
    let utilisateurs = hit("GET", "/admin/users", 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;
    println!("   ↳ {} utilisateurs récupérés", utilisateurs.as_array().unwrap().len());

    let u = utilisateurs.as_array().unwrap()[0].clone();
    let nom_modifie = "Admin_de_test_modifie";
    let updated_user_data = json!({ "name": nom_modifie });
    hit("PATCH", &format!("/admin/users/{}", u.get("id").unwrap().as_u64().unwrap() as u32), 200, Some(updated_user_data), who).await.map_err(|e| AppError::DatabaseError(e))?
;

    assert_eq(&hit("GET", &format!("/users/{}", u.get("id").unwrap().as_u64().unwrap() as u32), 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
, "name", nom_modifie);

    Ok(())
}

async fn test_auth_me(who: &str) -> Result<(), Box<dyn Error>> {
    println!("\n📌 TEST MODULE: AUTH /me");
    let me = hit("GET", "/auth/me", 200, None, who).await.map_err(|e| AppError::DatabaseError(e))?
;
    if me.get("email").is_none() || me.get("name").is_none() {
        return Err("Réponse invalide pour /auth/me".into());
    }
    println!("   ↳ Utilisateur connecté: {} ({})", me.get("email").unwrap().as_str().unwrap(), me.get("name").unwrap().as_str().unwrap());

    Ok(())
}

// Exécution des tests
#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    println!("🧪 Démarrage des tests\n");

    let config = Config::new();
    let cookie_jars = CookieJars::new();

    login(&config.admin_email, &config.admin_password, "admin").await.map_err(|e| AppError::DatabaseError(e))?
;
    let user_email = "utilisateur_de_test@example.com";
    register_user("User", user_email, "pass123", "user").await.map_err(|e| AppError::DatabaseError(e))?
;

    test_plants("admin").await.map_err(|e| AppError::DatabaseError(e))?
;
    test_users("admin").await.map_err(|e| AppError::DatabaseError(e))?
;
    test_orders("admin", "user").await.map_err(|e| AppError::DatabaseError(e))?
;
    test_user_profile("admin", "user", user_email).await.map_err(|e| AppError::DatabaseError(e))?
;
    test_auth_roles("admin", "user").await.map_err(|e| AppError::DatabaseError(e))?
;
    test_admin_plants("admin").await.map_err(|e| AppError::DatabaseError(e))?
;
    test_admin_users("admin").await.map_err(|e| AppError::DatabaseError(e))?
;
    test_auth_me("user").await.map_err(|e| AppError::DatabaseError(e))?
;

    println!("\n🎉 Tous les tests ont réussi!");
    Ok(())
}
