use crate::errors::AppError;
use bigdecimal::BigDecimal;
use dotenvy::dotenv;
use rand::seq::SliceRandom;
use rand::Rng;
use sqlx::{Pool, Postgres, Transaction};
use std::env;
use std::fs::File;
use std::io::Write;
use uuid::Uuid;

// # Constantes
const NB_ADMINS: u32 = 3;
const NB_USERS: u32 = 20;
const NB_PLANTS: u32 = 50;
const MAX_ORDERS_PER_USER: u32 = 7;
const PLANT_NAMES: &[&str] = &[
	"Rose","Tulipe","Lavande","Orchidée","Basilic","Menthe","Pivoine","Tournesol","Cactus (Echinopsis)",
	"Bambou","Camomille (Matricaria recutita)","Sauge (Salvia officinalis)","Romarin (Rosmarinus officinalis)",
	"Thym (Thymus vulgaris)","Laurier-rose (Nerium oleander)","Aloe vera","Jasmin (Jasminum officinale)",
	"Hortensia (Hydrangea macrophylla)","Marguerite (Leucanthemum vulgare)","Géranium (Pelargonium graveolens)",
	"Fuchsia (Fuchsia magellanica)","Anémone (Anemone coronaria)","Azalée (Rhododendron simsii)",
	"Chrysanthème (Chrysanthemum morifolium)","Digitale pourpre (Digitalis purpurea)","Glaïeul (Gladiolus hortulanus)",
	"Lys (Lilium candidum)","Violette (Viola odorata)","Muguet (Convallaria majalis)","Iris (Iris germanica)",
	"Lavandin (Lavandula intermedia)","Érable du Japon (Acer palmatum)","Citronnelle (Cymbopogon citratus)",
	"Pin parasol (Pinus pinea)","Cyprès (Cupressus sempervirens)","Olivier (Olea europaea)","Papyrus (Cyperus papyrus)",
	"Figuier (Ficus carica)","Eucalyptus (Eucalyptus globulus)","Acacia (Acacia dealbata)","Bégonia (Begonia semperflorens)",
	"Calathea (Calathea ornata)","Dieffenbachia (Dieffenbachia seguine)","Ficus elastica","Sansevieria (Sansevieria trifasciata)",
	"Philodendron (Philodendron scandens)","Yucca (Yucca elephantipes)","Zamioculcas zamiifolia","Monstera deliciosa",
	"Pothos (Epipremnum aureum)","Agave (Agave americana)","Cactus raquette (Opuntia ficus-indica)",
	"Palmier-dattier (Phoenix dactylifera)","Amaryllis (Hippeastrum hybridum)","Bleuet (Centaurea cyanus)",
	"Cœur-de-Marie (Lamprocapnos spectabilis)","Croton (Codiaeum variegatum)","Dracaena (Dracaena marginata)",
	"Hosta (Hosta plantaginea)","Lierre (Hedera helix)","Mimosa (Acacia dealbata)",
];

// Structures temporaires
#[derive(Clone)]
struct TempPlant {
	id: Uuid,
	price: BigDecimal,
	stock: i32,
}

struct TempUser {
	id: Uuid,
}

/// Nettoyage complet des tables (sans DROP)
async fn reset_db(pool: &Pool<Postgres>) -> Result<(), AppError> {
	println!("🧹 Nettoyage de la base de données...");
	sqlx::query!("DELETE FROM order_items").execute(pool).await?;
	sqlx::query!("DELETE FROM orders").execute(pool).await?;
	sqlx::query!("DELETE FROM plants").execute(pool).await?;
	sqlx::query!("DELETE FROM users").execute(pool).await?;
	println!("✅ Base de données nettoyée.");
	Ok(())
}

/// Génère un email réaliste (prénom.nom[numéro]@[fournisseur])
fn generate_realistic_email(index: u32) -> String {
	let prenoms = ["charles", "brain", "roy", "zackary", "vincenza", "kyle", "christelle", "berenice", "greg", "bart", "maybelle", "amanda", "gabe", "brooklyn", "tanner", "malachi", "dana", "kaelyn", "nickolas", "kathryne"];
	let noms = ["lubowitz", "bernier", "tremblay", "gusikowski", "mohr", "cormier", "wolf", "mraz", "blick", "wisoky", "prohaska"];
	let domaines = ["gmail.com", "yahoo.com", "hotmail.com"];
	let prenom = prenoms[(index as usize) % prenoms.len()];
	let nom = noms[(index as usize) % noms.len()];
	let numero = 20 + index;
	let domaine = domaines[(index as usize) % domaines.len()];
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
	cost: u32,
) -> Result<(Vec<(String, String)>, Vec<(String, String)>, Vec<TempUser>), AppError> {
	println!("👤 Création des utilisateurs et admins...");

	// ─── Transaction locale pour regrouper les insertions ─────────────
	let mut tx: Transaction<'_, Postgres> = pool.begin().await?;

	let mut admins_creds = Vec::new();
	let mut users_creds = Vec::new();
	let mut temp_users = Vec::new();

	// Admins
	for index in 1..=NB_ADMINS {
		let email = format!("admin{}@planteshop.com", index);
		let password = "password".to_string();
		let password_hash = bcrypt::hash(&password, cost).map_err(|_| AppError::Internal)?;
		let user = sqlx::query_as!(
			TempUser,
			"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1, $2, $3, true) RETURNING id",
			email,
			format!("admin{}", index),
			password_hash
		)
		.fetch_one(&mut *tx).await?;
		temp_users.push(user);
		admins_creds.push((email, password));
	}

	// Users
	for index in 0..NB_USERS {
		let email = generate_realistic_email(index);
		let password = generate_random_password();
		let password_hash = bcrypt::hash(&password, cost).map_err(|_| AppError::Internal)?;
		let username = email.split('@').next().unwrap_or("user").replace('.', "_");
		let user = sqlx::query_as!(
			TempUser,
			"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1, $2, $3, false) RETURNING id",
			email,
			username,
			password_hash
		)
		.fetch_one(&mut *tx).await?;
		temp_users.push(user);
		users_creds.push((email, password));
	}

	tx.commit().await?;
	println!("✅ {} utilisateurs créés.", temp_users.len());
	Ok((admins_creds, users_creds, temp_users))
}

/// Crée un lot de plantes et renvoie leurs infos (id, price, stock)
async fn create_plants(pool: &Pool<Postgres>) -> Result<Vec<TempPlant>, AppError> {
	println!("🌱 Création des plantes...");
	let mut rng = rand::thread_rng();
	let mut temp_plants = Vec::new();

	// ─── Transaction locale pour regrouper les insertions ─────────────
	let mut tx: Transaction<'_, Postgres> = pool.begin().await?;

	for idx in 0..NB_PLANTS {
		let name = PLANT_NAMES[idx as usize % PLANT_NAMES.len()].to_string();
		let price = BigDecimal::from(rng.gen_range(5..51));
		let stock = rng.gen_range(5..31);
		let description = format!("Une description pour la plante {}.", name);

		let plant = sqlx::query_as!(
			TempPlant,
			"INSERT INTO plants (name, description, price, stock) VALUES ($1, $2, $3, $4)
			 RETURNING id, price, stock",
			name, description, price, stock
		)
		.fetch_one(&mut *tx).await?;
		temp_plants.push(plant);
	}

	tx.commit().await?;
	println!("✅ {} plantes créées.", temp_plants.len());
	Ok(temp_plants)
}

/// Crée des commandes aléatoires pour chaque user
async fn create_orders(
	pool: &Pool<Postgres>,
	users: Vec<TempUser>,
	mut plants: Vec<TempPlant>
) -> Result<(), AppError> {
	println!("🛒 Création des commandes...");
	let mut rng = rand::thread_rng();
	let statuses = ["pending", "confirmed", "shipped", "delivered"];
	let mut total_orders = 0;

	// ─── Transaction locale pour regrouper inserts/updates ────────────
	let mut tx: Transaction<'_, Postgres> = pool.begin().await?;

	for user in users {
		let num_orders = rng.gen_range(0..=MAX_ORDERS_PER_USER);
		for _ in 0..num_orders {
			let status = statuses.choose(&mut rng).unwrap().to_string();
			let order = sqlx::query!(
				"INSERT INTO orders (user_id, total, status) VALUES ($1, 0, $2) RETURNING id",
				user.id, status
			)
			.fetch_one(&mut *tx).await?;

			let mut order_total = BigDecimal::from(0);
			let num_items = rng.gen_range(1..=3);

			for _ in 0..num_items {
				if let Some(plant) = plants.choose_mut(&mut rng) {
					if plant.stock > 0 {
						let quantity = rng.gen_range(1..=std::cmp::min(5, plant.stock));

						sqlx::query!(
							"INSERT INTO order_items (order_id, plant_id, quantity, price)
							 VALUES ($1, $2, $3, $4)",
							order.id, plant.id, quantity, plant.price
						)
						.execute(&mut *tx).await?;

						sqlx::query!(
							"UPDATE plants SET stock = stock - $1 WHERE id = $2",
							quantity, plant.id
						)
						.execute(&mut *tx).await?;

						plant.stock -= quantity;
						order_total += &plant.price * BigDecimal::from(quantity);
					}
				}
			}

			sqlx::query!(
				"UPDATE orders SET total = $1 WHERE id = $2",
				order_total, order.id
			)
			.execute(&mut *tx).await?;
			total_orders += 1;
		}
	}

	tx.commit().await?;
	println!("✅ {} commandes créées.", total_orders);
	Ok(())
}

/// Écrit un fichier users.txt avec les identifiants
fn write_users_file(
	admins: Vec<(String, String)>,
	users: Vec<(String, String)>
) -> Result<(), AppError> {
	println!("✍️  Génération du fichier users.txt...");
	let mut file = File::create("users.txt").map_err(|_| AppError::Internal)?;
	let mut content = String::from("Administrateurs :\n\n");
	for (email, password) in admins {
		content.push_str(&format!("{} {}\n", email, password));
	}
	content.push_str("\nUtilisateurs :\n\n");
	for (email, password) in users {
		content.push_str(&format!("{} {}\n", email, password));
	}
	file.write_all(content.as_bytes()).map_err(|_| AppError::Internal)?;
	println!("✅ Fichier users.txt généré.");
	Ok(())
}

/// Point d'entrée du seed (appelé depuis src/bin/seed.rs)
pub async fn run_seed() -> Result<(), AppError> {
	dotenv().ok();
	let database_url = env::var("DATABASE_URL").map_err(|_| AppError::Internal)?;
	let bcrypt_cost = env::var("BCRYPT_COST_SEED")
		.ok()
		.and_then(|s| s.parse::<u32>().ok())
		.or_else(|| env::var("BCRYPT_COST").ok().and_then(|s| s.parse::<u32>().ok()))
		.unwrap_or(8);

	println!("🔌 Connexion à la base de données...");
	let pool = sqlx::PgPool::connect(&database_url).await?;
	println!("✅ Connecté.");

	reset_db(&pool).await?;
	let plants = create_plants(&pool).await?;
	let (admins_creds, users_creds, temp_users) = create_users(&pool, bcrypt_cost).await?;
	write_users_file(admins_creds, users_creds)?;
	create_orders(&pool, temp_users, plants).await?;

	println!("\n🎉 Seed terminé avec succès !");
	Ok(())
}
