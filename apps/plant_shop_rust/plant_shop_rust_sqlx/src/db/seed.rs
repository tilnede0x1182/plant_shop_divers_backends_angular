//! Seed de la base de donnees.

#![allow(dead_code)]

// ==============================================================================
// Importations
// ==============================================================================

use crate::errors::AppError;
use argon2::password_hash::{rand_core::OsRng, SaltString};
use argon2::{Argon2, PasswordHasher};
use bigdecimal::BigDecimal;
use dotenvy::dotenv;
use lipsum;
use rand::seq::SliceRandom;
use rand::Rng;
use sqlx::{Pool, Postgres};
use std::env;
use std::fs::File;
use std::io::Write;

// ==============================================================================
// Constantes
// ==============================================================================

const NB_ADMINS: u32 = 3;
const NB_USERS: u32 = 20;
const NB_PLANTS: u32 = 50;
const MAX_ORDERS_PER_USER: u32 = 7;

const EMAIL_PRENOMS: &[&str] = &[
    "charles", "brain", "roy", "zackary", "vincenza", "kyle", "christelle",
    "berenice", "greg", "bart", "maybelle", "amanda", "gabe", "brooklyn",
    "tanner", "malachi", "dana", "kaelyn", "nickolas", "kathryne",
];

const EMAIL_NOMS: &[&str] = &[
    "lubowitz", "bernier", "tremblay", "gusikowski", "mohr",
    "cormier", "wolf", "mraz", "blick", "wisoky", "prohaska",
];

const EMAIL_DOMAINES: &[&str] = &["gmail.com", "yahoo.com", "hotmail.com"];

const PLANT_NAMES: &[&str] = &[
    "Rose",
    "Tulipe",
    "Lavande",
    "Orchidée",
    "Basilic",
    "Menthe",
    "Pivoine",
    "Tournesol",
    "Cactus (Echinopsis)",
    "Bambou",
    "Camomille (Matricaria recutita)",
    "Sauge (Salvia officinalis)",
    "Romarin (Rosmarinus officinalis)",
    "Thym (Thymus vulgaris)",
    "Laurier-rose (Nerium oleander)",
    "Aloe vera",
    "Jasmin (Jasminum officinale)",
    "Hortensia (Hydrangea macrophylla)",
    "Marguerite (Leucanthemum vulgare)",
    "Géranium (Pelargonium graveolens)",
    "Fuchsia (Fuchsia magellanica)",
    "Anémone (Anemone coronaria)",
    "Azalée (Rhododendron simsii)",
    "Chrysanthème (Chrysanthemum morifolium)",
    "Digitale pourpre (Digitalis purpurea)",
    "Glaïeul (Gladiolus hortulanus)",
    "Lys (Lilium candidum)",
    "Violette (Viola odorata)",
    "Muguet (Convallaria majalis)",
    "Iris (Iris germanica)",
    "Lavandin (Lavandula intermedia)",
    "Érable du Japon (Acer palmatum)",
    "Citronnelle (Cymbopogon citratus)",
    "Pin parasol (Pinus pinea)",
    "Cyprès (Cupressus sempervirens)",
    "Olivier (Olea europaea)",
    "Papyrus (Cyperus papyrus)",
    "Figuier (Ficus carica)",
    "Eucalyptus (Eucalyptus globulus)",
    "Acacia (Acacia dealbata)",
    "Bégonia (Begonia semperflorens)",
    "Calathea (Calathea ornata)",
    "Dieffenbachia (Dieffenbachia seguine)",
    "Ficus elastica",
    "Sansevieria (Sansevieria trifasciata)",
    "Philodendron (Philodendron scandens)",
    "Yucca (Yucca elephantipes)",
    "Zamioculcas zamiifolia",
    "Monstera deliciosa",
    "Pothos (Epipremnum aureum)",
    "Agave (Agave americana)",
    "Cactus raquette (Opuntia ficus-indica)",
    "Palmier-dattier (Phoenix dactylifera)",
    "Amaryllis (Hippeastrum hybridum)",
    "Bleuet (Centaurea cyanus)",
    "Cœur-de-Marie (Lamprocapnos spectabilis)",
    "Croton (Codiaeum variegatum)",
    "Dracaena (Dracaena marginata)",
    "Hosta (Hosta plantaginea)",
    "Lierre (Hedera helix)",
    "Mimosa (Acacia dealbata)",
];

// ==============================================================================
// Structures
// ==============================================================================

/// Structure temporaire pour le seed des plantes.
#[derive(Clone)]
struct TempPlant {
    id: i32,
    price: BigDecimal,
    stock: i32,
}

/// Structure temporaire pour le seed des utilisateurs.
struct TempUser {
    id: i32,
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

/// Exécute une requête SQL, affiche l’erreur sans panique.
/// @pool   Connexion/Pool
/// @query  Requête SQL (texte)
/// @tag    Tag log pour affichage
async fn safe_execute(pool: &Pool<Postgres>, query: &str, tag: &str) {
    if let Err(e) = sqlx::query(query).execute(pool).await {
        println!("[ERREUR][{tag}] {e}");
    }
}

/// Nettoyage complet des tables (sans DROP)
async fn reset_db(pool: &Pool<Postgres>) -> Result<(), AppError> {
    println!("🧹 Nettoyage de la base de données...");
    safe_execute(pool, "DELETE FROM order_items", "order_items").await;
    safe_execute(pool, "DELETE FROM orders", "orders").await;
    safe_execute(pool, "DELETE FROM plants", "plants").await;
    safe_execute(pool, "DELETE FROM users", "users").await;
    println!("✅ Base de données nettoyée.");
    Ok(())
}

/// Genere un email realiste (prenom.nom[numero]@[fournisseur]).
///
/// @param index Index pour varier les donnees
/// @return Email genere
fn generate_realistic_email(index: u32) -> String {
    let prenom = EMAIL_PRENOMS[(index as usize) % EMAIL_PRENOMS.len()];
    let nom = EMAIL_NOMS[(index as usize) % EMAIL_NOMS.len()];
    let numero = 20 + index;
    let domaine = EMAIL_DOMAINES[(index as usize) % EMAIL_DOMAINES.len()];
    format!("{}_{}{}@{}", prenom, nom, numero, domaine)
}

/// Génère un mot de passe aléatoire de 12 caractères
fn generate_random_password() -> String {
    use rand::distributions::Alphanumeric;
    use rand::{thread_rng, Rng};
    thread_rng()
        .sample_iter(&Alphanumeric)
        .take(12)
        .map(char::from)
        .collect()
}

/// Crée admins + users et renvoie (creds_admins, creds_users, users_temp)
async fn create_users(
    pool: &Pool<Postgres>,
) -> Result<(Vec<(String, String)>, Vec<(String, String)>, Vec<TempUser>), AppError> {
    println!("👤 Création des utilisateurs et admins...");

    let mut admins_creds = Vec::new();
    let mut users_creds = Vec::new();
    let mut temp_users = Vec::new();

    let prenoms = [
        "Jean",
        "Marie",
        "Luc",
        "Sophie",
        "Pierre",
        "Camille",
        "Thomas",
        "Julie",
        "Louis",
        "Élise",
        "Nicolas",
        "Chloé",
        "Antoine",
        "Sarah",
        "Maxime",
        "Laura",
        "Hugo",
        "Claire",
        "Alexandre",
        "Manon",
    ];
    let noms = [
        "Dupont", "Durand", "Martin", "Bernard", "Petit", "Robert", "Richard", "Garcia", "Leroy",
        "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "David", "Bertrand", "Roux", "Vincent",
        "Fournier", "Girard",
    ];
    let mut rng = rand::thread_rng();

    // Admins
    for index in 1..=NB_ADMINS {
        let email = format!("admin{}@planteshop.com", index);
        let password = "password".to_string();
        let salt = SaltString::generate(&mut OsRng);
        let password_hash = Argon2::default()
            .hash_password(password.as_bytes(), &salt)
            .map_err(|_| AppError::Internal)?
            .to_string();
        let prenom = prenoms[rng.gen_range(0..prenoms.len())];
        let nom = noms[rng.gen_range(0..noms.len())];
        let full_name = format!("{} {}", prenom, nom);

        let row = sqlx::query!(
            "INSERT INTO users (email, username, password_hash, is_admin)
			VALUES ($1, $2, $3, true) RETURNING id",
            email,
            full_name,
            password_hash
        )
        .fetch_one(pool)
        .await?;

        temp_users.push(TempUser { id: row.id });
        admins_creds.push((email, password));
    }

    // Users
    for index in 0..NB_USERS {
        let email = generate_realistic_email(index);
        let password = generate_random_password();
        let salt = SaltString::generate(&mut OsRng);
        let password_hash = Argon2::default()
            .hash_password(password.as_bytes(), &salt)
            .map_err(|_| AppError::Internal)?
            .to_string();
        let prenom = prenoms[rng.gen_range(0..prenoms.len())];
        let nom = noms[rng.gen_range(0..noms.len())];
        let full_name = format!("{} {}", prenom, nom);

        let row = sqlx::query!(
            "INSERT INTO users (email, username, password_hash, is_admin)
			VALUES ($1, $2, $3, false) RETURNING id",
            email,
            full_name,
            password_hash
        )
        .fetch_one(pool)
        .await?;

        temp_users.push(TempUser { id: row.id });
        users_creds.push((email, password));
    }

    println!("✅ {} utilisateurs créés.", (NB_ADMINS + NB_USERS));
    Ok((admins_creds, users_creds, temp_users))
}

/// Crée un lot de plantes et renvoie leurs infos (id, price, stock)
async fn create_plants(pool: &Pool<Postgres>) -> Result<Vec<TempPlant>, AppError> {
    println!("🌱 Création des plantes...");
    let mut rng = rand::thread_rng();
    let mut temp_plants = Vec::new();

    let max = PLANT_NAMES.len() as u32;
    for idx in 0..NB_PLANTS {
        let base = PLANT_NAMES[idx as usize % max as usize];
        let name = base.to_string();
        let price = BigDecimal::from(rng.gen_range(5..51));
        let stock = rng.gen_range(5..31);
        let desc_len = rng.gen_range(10..15);
        let description = lipsum::lipsum_words_with_rng(&mut rng, desc_len);

        // Ajout de gestion d'erreur explicite
        match sqlx::query!(
			"INSERT INTO plants (name, description, price, stock) VALUES ($1, $2, $3, $4) RETURNING id",
			name, description, price, stock
		)
        .fetch_one(pool)
        .await
        {
            Ok(row) => {
                temp_plants.push(TempPlant {
                    id: row.id,
                    price: price.clone(),
                    stock,
                });
                // println!("  ✓ Plante {} créée (id: {})", name, row.id);
            }
            Err(e) => {
                println!("  ✗ ERREUR lors de la création de '{}': {}", name, e);
                return Err(e.into());
            }
        }
    }

    println!("✅ {} plantes créées.", temp_plants.len());
    Ok(temp_plants)
}

/// Crée des commandes aléatoires pour chaque user
async fn create_orders(
    pool: &Pool<Postgres>,
    users: Vec<TempUser>,
    mut plants: Vec<TempPlant>,
) -> Result<(), AppError> {
    println!("🛒 Création des commandes...");
    let mut rng = rand::thread_rng();
    let statuses = ["pending", "confirmed", "shipped", "delivered"];
    let mut total_orders = 0;

    for user in users {
        let num_orders = rng.gen_range(0..=MAX_ORDERS_PER_USER);
        for _ in 0..num_orders {
            let status = statuses.choose(&mut rng).unwrap().to_string();
            let row = sqlx::query!(
                "INSERT INTO orders (user_id, total, status) VALUES ($1, 0, $2) RETURNING id",
                user.id,
                status
            )
            .fetch_one(pool)
            .await?;

            let mut order_total = BigDecimal::from(0);
            let num_items = rng.gen_range(1..=3);

            for _ in 0..num_items {
                if let Some(plant) = plants.choose_mut(&mut rng) {
                    if plant.stock > 0 {
                        let quantity = rng.gen_range(1..=std::cmp::min(5, plant.stock));

                        sqlx::query!(
                            "INSERT INTO order_items (order_id, plant_id, quantity, price)
							 VALUES ($1, $2, $3, $4)",
                            row.id,
                            plant.id,
                            quantity,
                            plant.price
                        )
                        .execute(pool)
                        .await?;

                        sqlx::query!(
                            "UPDATE plants SET stock = stock - $1 WHERE id = $2",
                            quantity,
                            plant.id
                        )
                        .execute(pool)
                        .await?;

                        plant.stock -= quantity;
                        order_total += &plant.price * BigDecimal::from(quantity);
                    }
                }
            }

            sqlx::query!(
                "UPDATE orders SET total = $1 WHERE id = $2",
                order_total,
                row.id
            )
            .execute(pool)
            .await?;
            total_orders += 1;
        }
    }

    println!("✅ {} commandes créées.", total_orders);
    Ok(())
}

/// Formate une liste de credentials en texte.
///
/// @param creds Liste de (email, password)
/// @return Texte formate
fn format_credentials(creds: Vec<(String, String)>) -> String {
    creds.iter()
        .map(|(email, password)| format!("{} {}
", email, password))
        .collect()
}

/// Ecrit le contenu dans un fichier avec gestion d'erreur.
///
/// @param file Fichier ouvert en ecriture
/// @param content Contenu a ecrire
fn write_file_content(file: &mut File, content: &str) {
    if let Err(e) = file.write_all(content.as_bytes()) {
        println!("[ERREUR][write users.txt] {e}");
    }
}

/// Cree un fichier et retourne le handle ou None en cas d'erreur.
///
/// @param path Chemin du fichier
/// @return Option<File> ou None
fn create_file_safe(path: &str) -> Option<File> {
    match File::create(path) {
        Ok(f) => Some(f),
        Err(e) => {
            println!("[ERREUR][create {path}] {e}");
            None
        }
    }
}

/// Ecrit un fichier users.txt avec les identifiants.
///
/// @param admins Credentials des admins (email, password)
/// @param users Credentials des users (email, password)
/// @return Ok(()) si reussi, Err(AppError) sinon
fn write_users_file(
    admins: Vec<(String, String)>,
    users: Vec<(String, String)>,
) -> Result<(), AppError> {
    println!("✍️  Génération du fichier users.txt...");
    let Some(mut file) = create_file_safe("users.txt") else { return Ok(()); };
    let content = format!("Administrateurs :\n\n{}\nUtilisateurs :\n\n{}",
        format_credentials(admins), format_credentials(users));
    write_file_content(&mut file, &content);
    println!("✅ Fichier users.txt généré.");
    Ok(())
}

// ==============================================================================
// Main
// ==============================================================================

/// Point d'entrée de la seed (appelé depuis src/bin/seed.rs)
pub async fn run_seed() -> Result<(), AppError> {
    dotenv().ok();
    let database_url = env::var("DATABASE_URL").map_err(|_| AppError::Internal)?;
    let _bcrypt_cost = env::var("BCRYPT_COST_SEED")
        .ok()
        .and_then(|s| s.parse::<u32>().ok())
        .or_else(|| {
            env::var("BCRYPT_COST")
                .ok()
                .and_then(|s| s.parse::<u32>().ok())
        })
        .unwrap_or(8);

    println!("🔌 Connexion à la base de données...");
    let pool = sqlx::PgPool::connect(&database_url).await?;
    println!("✅ Connecté.");

    reset_db(&pool).await?;
    let plants = create_plants(&pool).await?;
    let (admins_creds, users_creds, temp_users) = create_users(&pool).await?;
    write_users_file(admins_creds, users_creds)?;
    create_orders(&pool, temp_users, plants).await?;

    println!("\n🎉 Seed terminée avec succès !");
    Ok(())
}
