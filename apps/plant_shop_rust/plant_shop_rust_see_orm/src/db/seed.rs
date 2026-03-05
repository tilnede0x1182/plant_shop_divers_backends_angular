//! Module de seed pour peupler la base de donnees avec des donnees de test.

#![allow(dead_code)]

// ==============================================================================
// Importations
// ==============================================================================

use crate::errors::AppError;
use argon2::password_hash::{rand_core::OsRng, SaltString};
use argon2::{Argon2, PasswordHasher};
use dotenvy::dotenv;
use lipsum;
use once_cell::sync::Lazy;
use rand::seq::SliceRandom;
use rand::Rng;
use sea_orm::{ConnectionTrait, Database, DatabaseConnection, DbBackend, Statement};
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
    price: i32,
    stock: i32,
}

/// Structure temporaire pour le seed des utilisateurs.
struct TempUser {
    id: i32,
}

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

/// Exécute une requête SQL, affiche l'erreur sans panique.
///
/// @param db Connexion SeaORM
/// @param query Requête SQL (texte)
/// @param tag Tag log pour affichage
async fn safe_execute(db: &DatabaseConnection, query: &str, tag: &str) {
    if let Err(e) = db
        .execute(Statement::from_string(
            DbBackend::Postgres,
            query.to_owned(),
        ))
        .await
    {
        println!("[ERREUR][{tag}] {e}");
    }
}

/// Nettoyage complet des tables (sans DROP).
///
/// @param db Connection SeaORM
/// @return Ok(()) si reussi, Err(AppError) sinon
async fn reset_db(db: &DatabaseConnection) -> Result<(), AppError> {
    println!("🧹 Nettoyage de la base de données...");
    safe_execute(db, "DELETE FROM order_items", "order_items").await;
    safe_execute(db, "DELETE FROM orders", "orders").await;
    safe_execute(db, "DELETE FROM plants", "plants").await;
    safe_execute(db, "DELETE FROM users", "users").await;
    println!("✅ Base de données nettoyée.");
    Ok(())
}

/// Genere un email realiste (prenom.nom[numero]@[fournisseur]).
///
/// @param index Index pour varier les donnees
/// @return Email genere
fn generate_realistic_email(index: u32) -> String {
    let prenoms = [
        "charles",
        "brain",
        "roy",
        "zackary",
        "vincenza",
        "kyle",
        "christelle",
        "berenice",
        "greg",
        "bart",
        "maybelle",
        "amanda",
        "gabe",
        "brooklyn",
        "tanner",
        "malachi",
        "dana",
        "kaelyn",
        "nickolas",
        "kathryne",
    ];
    let noms = [
        "lubowitz",
        "bernier",
        "tremblay",
        "gusikowski",
        "mohr",
        "cormier",
        "wolf",
        "mraz",
        "blick",
        "wisoky",
        "prohaska",
    ];
    let domaines = ["gmail.com", "yahoo.com", "hotmail.com"];
    let prenom = prenoms[(index as usize) % prenoms.len()];
    let nom = noms[(index as usize) % noms.len()];
    let numero = 20 + index;
    let domaine = domaines[(index as usize) % domaines.len()];
    format!("{}_{}{}@{}", prenom, nom, numero, domaine)
}

/// Genere un mot de passe aleatoire de 12 caracteres.
///
/// @return Mot de passe aleatoire
fn generate_random_password() -> String {
    use rand::distributions::Alphanumeric;
    use rand::{thread_rng, Rng};
    thread_rng()
        .sample_iter(&Alphanumeric)
        .take(12)
        .map(char::from)
        .collect()
}

/// Cree admins + users et renvoie leurs credentials.
///
/// @param db Connection SeaORM
/// @return Tuple (creds_admins, creds_users, users_temp) ou erreur
async fn create_users(
    db: &DatabaseConnection,
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
        let password_hash = ARGON2
            .hash_password(password.as_bytes(), &salt)
            .map_err(|_| AppError::Internal)?
            .to_string();
        let prenom = prenoms[rng.gen_range(0..prenoms.len())];
        let nom = noms[rng.gen_range(0..noms.len())];
        let full_name = format!("{} {}", prenom, nom);

        let stmt = Statement::from_sql_and_values(
            DbBackend::Postgres,
            r#"INSERT INTO users (email, username, password_hash, is_admin)
			   VALUES ($1, $2, $3, true) RETURNING id"#,
            vec![
                email.clone().into(),
                full_name.clone().into(),
                password_hash.clone().into(),
            ],
        );
        let row = db.query_one(stmt).await?.unwrap();
        let id: i32 = row.try_get("", "id")?;
        temp_users.push(TempUser { id });
        admins_creds.push((email, password));
    }

    // Users
    for index in 0..NB_USERS {
        let email = generate_realistic_email(index);
        let password = generate_random_password();
        let salt = SaltString::generate(&mut OsRng);
        let password_hash = ARGON2
            .hash_password(password.as_bytes(), &salt)
            .map_err(|_| AppError::Internal)?
            .to_string();
        let prenom = prenoms[rng.gen_range(0..prenoms.len())];
        let nom = noms[rng.gen_range(0..noms.len())];
        let full_name = format!("{} {}", prenom, nom);

        let stmt = Statement::from_sql_and_values(
            DbBackend::Postgres,
            r#"INSERT INTO users (email, username, password_hash, is_admin)
			   VALUES ($1, $2, $3, false) RETURNING id"#,
            vec![
                email.clone().into(),
                full_name.clone().into(),
                password_hash.clone().into(),
            ],
        );
        let row = db.query_one(stmt).await?.unwrap();
        let id: i32 = row.try_get("", "id")?;
        temp_users.push(TempUser { id });
        users_creds.push((email, password));
    }

    println!("✅ {} utilisateurs créés.", (NB_ADMINS + NB_USERS));
    Ok((admins_creds, users_creds, temp_users))
}

/// Cree un lot de plantes et renvoie leurs infos.
///
/// @param db Connection SeaORM
/// @return Vec<TempPlant> (id, price, stock) ou erreur
async fn create_plants(db: &DatabaseConnection) -> Result<Vec<TempPlant>, AppError> {
    println!("🌱 Création des plantes...");
    let mut rng = rand::thread_rng();
    let mut temp_plants = Vec::new();

    let max = PLANT_NAMES.len() as u32;
    for idx in 0..NB_PLANTS {
        let base = PLANT_NAMES[idx as usize % max as usize];
        let name = base.to_string();
        let price = rng.gen_range(5..51);
        let stock = rng.gen_range(5..31);
        let desc_len = rng.gen_range(10..15);
        let description = lipsum::lipsum_words_with_rng(&mut rng, desc_len);

        let stmt = Statement::from_sql_and_values(
            DbBackend::Postgres,
            r#"INSERT INTO plants (name, description, price, stock)
					VALUES ($1, $2, $3, $4) RETURNING id"#,
            vec![
                name.clone().into(),
                description.clone().into(),
                price.into(),
                stock.into(),
            ],
        );

        match db.query_one(stmt).await {
            Ok(Some(row)) => {
                let id: i32 = row.try_get("", "id")?;
                temp_plants.push(TempPlant { id, price, stock });
            }
            Ok(None) => {
                println!("⚠️ Aucune ligne retournée pour {name}");
            }
            Err(e) => {
                println!("✗ ERREUR lors de la création de '{name}': {e}");
                return Err(AppError::Internal);
            }
        }
    }

    println!("✅ {} plantes créées.", temp_plants.len());
    Ok(temp_plants)
}

/// Cree des commandes aleatoires pour chaque user.
///
/// @param db Connection SeaORM
/// @param users Liste des utilisateurs temporaires
/// @param plants Liste des plantes avec stock disponible
/// @return Ok(()) si reussi, Err(AppError) sinon
async fn create_orders(
    db: &DatabaseConnection,
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

            let stmt = Statement::from_sql_and_values(
                DbBackend::Postgres,
                r#"INSERT INTO orders (user_id, total, status)
				   VALUES ($1, 0, $2) RETURNING id"#,
                vec![user.id.into(), status.into()],
            );
            let row = db.query_one(stmt).await?.unwrap();
            let order_id: i32 = row.try_get("", "id")?;

            let mut order_total = 0;
            let num_items = rng.gen_range(1..=3);

            for _ in 0..num_items {
                if let Some(plant) = plants.choose_mut(&mut rng) {
                    if plant.stock > 0 {
                        let quantity = rng.gen_range(1..=std::cmp::min(5, plant.stock));

                        db.execute(Statement::from_sql_and_values(
                            DbBackend::Postgres,
                            r#"INSERT INTO order_items (order_id, plant_id, quantity, price)
							   VALUES ($1, $2, $3, $4)"#,
                            vec![
                                order_id.into(),
                                plant.id.into(),
                                quantity.into(),
                                plant.price.into(),
                            ],
                        ))
                        .await?;

                        db.execute(Statement::from_sql_and_values(
                            DbBackend::Postgres,
                            r#"UPDATE plants SET stock = stock - $1 WHERE id = $2"#,
                            vec![quantity.into(), plant.id.into()],
                        ))
                        .await?;

                        plant.stock -= quantity;
                        order_total += plant.price * quantity;
                    }
                }
            }

            db.execute(Statement::from_sql_and_values(
                DbBackend::Postgres,
                r#"UPDATE orders SET total = $1 WHERE id = $2"#,
                vec![order_total.into(), order_id.into()],
            ))
            .await?;

            total_orders += 1;
        }
    }

    println!("✅ {} commandes créées.", total_orders);
    Ok(())
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
    let mut file = match File::create("users.txt") {
        Ok(f) => f,
        Err(e) => {
            println!("[ERREUR][create users.txt] {e}");
            return Ok(());
        }
    };
    let mut content = String::from("Administrateurs :\n\n");
    for (email, password) in admins {
        content.push_str(&format!("{} {}\n", email, password));
    }
    content.push_str("\nUtilisateurs :\n\n");
    for (email, password) in users {
        content.push_str(&format!("{} {}\n", email, password));
    }
    if let Err(e) = file.write_all(content.as_bytes()) {
        println!("[ERREUR][write users.txt] {e}");
    }
    println!("✅ Fichier users.txt généré.");
    Ok(())
}

/// Point d'entree de la seed (appele depuis src/bin/seed.rs).
///
/// @return Ok(()) si seed reussie, Err(AppError) sinon
pub async fn run_seed() -> Result<(), AppError> {
    dotenv().ok();
    let database_url = env::var("DATABASE_URL").map_err(|_| AppError::Internal)?;

    println!("🔌 Connexion à la base de données...");
    let db: DatabaseConnection = Database::connect(&database_url).await?;
    println!("✅ Connecté.");

    reset_db(&db).await?;
    let plants = create_plants(&db).await?;
    let (admins_creds, users_creds, temp_users) = create_users(&db).await?;
    write_users_file(admins_creds, users_creds)?;
    create_orders(&db, temp_users, plants).await?;

    println!("\n🎉 Seed terminée avec succès !");
    Ok(())
}
static ARGON2: Lazy<Argon2> = Lazy::new(Argon2::default);
