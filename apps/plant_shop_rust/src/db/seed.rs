use crate::errors::AppError;
use bigdecimal::BigDecimal;
use dotenvy::dotenv;
use rand::seq::SliceRandom;
use rand::Rng;
use sqlx::{postgres::PgPoolOptions, Pool, Postgres, Transaction};
use std::env;
use std::fs::File;
use std::io::Write;
use uuid::Uuid;

//
// # Données et constantes
//
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

//
// # Structures temporaires
//
#[derive(Clone)]
struct TempPlant {
	id: Uuid,
	price: BigDecimal,
	stock: i32,
}

struct TempUser {
	id: Uuid,
}

//
// # Fonctions utilitaires
//

/// Récupère un coût bcrypt rapide pour la seed.
/// @retour coût bcrypt
fn bcrypt_cost_for_seed() -> u32 {
	env::var("BCRYPT_COST_SEED")
		.ok()
		.and_then(|valeur| valeur.parse::<u32>().ok())
		.or_else(|| env::var("BCRYPT_COST").ok().and_then(|v| v.parse::<u32>().ok()))
		.unwrap_or(8)
}

/// Génère un email réaliste (prenom_nomNN@domaine).
/// @index indice utilisateur
/// @retour email
fn generate_realistic_email(index: u32) -> String {
	let prenoms = ["charles","brain","roy","zackary","vincenza","kyle","christelle","berenice","greg","bart",
		"maybelle","amanda","gabe","brooklyn","tanner","malachi","dana","kaelyn","nickolas","kathryne"];
	let noms = ["lubowitz","bernier","tremblay","gusikowski","mohr","cormier","wolf","mraz","blick","wisoky","prohaska"];
	let domaines = ["gmail.com","yahoo.com","hotmail.com"];
	let prenom = prenoms[(index as usize) % prenoms.len()];
	let nom = noms[(index as usize) % noms.len()];
	let numero = 20 + index;
	let domaine = domaines[(index as usize) % domaines.len()];
	format!("{}_{}{}@{}", prenom, nom, numero, domaine)
}

/// Génère un mot de passe aléatoire de 12 caractères.
/// @retour mot de passe
fn generate_random_password() -> String {
	use rand::distributions::Alphanumeric;
	use rand::{thread_rng, Rng};
	thread_rng().sample_iter(&Alphanumeric).take(12).map(char::from).collect()
}

/// Construit un username simple à partir d'un email.
/// @email email source
/// @retour username
fn username_from_email(email: &str) -> String {
	email.split('@').next().unwrap_or("user").replace('.', "_")
}

//
// # Fonctions utilitaires principales
//

/// Supprime les données (transaction).
/// @tx transaction ouverte
async fn reset_db_tx(tx: &mut Transaction<'_, Postgres>) -> Result<(), AppError> {
	sqlx::query!("DELETE FROM order_items").execute(&mut *tx).await?;
	sqlx::query!("DELETE FROM orders").execute(&mut *tx).await?;
	sqlx::query!("DELETE FROM plants").execute(&mut *tx).await?;
	sqlx::query!("DELETE FROM users").execute(&mut *tx).await?;
	Ok(())
}

/// Insère un admin et retourne son id.
/// @tx transaction ouverte @index numéro d’admin @cost coût bcrypt
async fn insert_admin(tx: &mut Transaction<'_, Postgres>, index: u32, cost: u32) -> Result<(TempUser, (String,String)), AppError> {
	let email = format!("admin{}@planteshop.com", index);
	let password = "password".to_string();
	let password_hash = bcrypt::hash(&password, cost).map_err(|_| AppError::Internal)?;
	let utilisateur = sqlx::query_as!(
		TempUser,
		"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1,$2,$3,true) RETURNING id",
		email, format!("admin{}", index), password_hash
	).fetch_one(&mut *tx).await?;
	Ok((utilisateur, (email, password)))
}

/// Insère un user et retourne son id + credentials.
/// @tx transaction ouverte @index indice user @cost coût bcrypt
async fn insert_user(tx: &mut Transaction<'_, Postgres>, index: u32, cost: u32) -> Result<(TempUser,(String,String)), AppError> {
	let email = generate_realistic_email(index);
	let username = username_from_email(&email);
	let password = generate_random_password();
	let password_hash = bcrypt::hash(&password, cost).map_err(|_| AppError::Internal)?;
	let utilisateur = sqlx::query_as!(
		TempUser,
		"INSERT INTO users (email, username, password_hash, is_admin) VALUES ($1,$2,$3,false) RETURNING id",
		email, username, password_hash
	).fetch_one(&mut *tx).await?;
	Ok((utilisateur, (email, password)))
}

/// Insère une plante et retourne ses infos.
/// @tx transaction ouverte @name nom @price prix @stock stock
async fn insert_plant(tx: &mut Transaction<'_, Postgres>, name: String, price: BigDecimal, stock: i32) -> Result<TempPlant, AppError> {
	let description = format!("Une description pour la plante {}.", name);
	let plante = sqlx::query_as!(
		TempPlant,
		"INSERT INTO plants (name, description, price, stock) VALUES ($1,$2,$3,$4) RETURNING id, price, stock",
		name, description, price, stock
	).fetch_one(&mut *tx).await?;
	Ok(plante)
}

/// Ajoute un item à une commande et met à jour le stock.
/// @tx transaction @order_id id commande @plant plante @qty quantité
async fn add_item_and_decrement_stock(tx: &mut Transaction<'_, Postgres>, order_id: Uuid, plant: &mut TempPlant, qty: i32) -> Result<BigDecimal, AppError> {
	sqlx::query!(
		"INSERT INTO order_items (order_id, plant_id, quantity, price) VALUES ($1,$2,$3,$4)",
		order_id, plant.id, qty, plant.price
	).execute(&mut *tx).await?;
	sqlx::query!("UPDATE plants SET stock = stock - $1 WHERE id = $2", qty, plant.id).execute(&mut *tx).await?;
	plant.stock -= qty;
	Ok((&plant.price * BigDecimal::from(qty)).into())
}

/// Met à jour le total d'une commande.
/// @tx transaction @order_id id commande @total total
async fn update_order_total(tx: &mut Transaction<'_, Postgres>, order_id: Uuid, total: BigDecimal) -> Result<(), AppError> {
	sqlx::query!("UPDATE orders SET total = $1 WHERE id = $2", total, order_id).execute(&mut *tx).await?;
	Ok(())
}

//
// # Fonctions principales
//

/// Crée tous les utilisateurs (admins + users).
/// @tx transaction ouverte @cost coût bcrypt
async fn create_users_tx(tx: &mut Transaction<'_, Postgres>, cost: u32) -> Result<(Vec<(String,String)>,Vec<(String,String)>,Vec<TempUser>), AppError> {
	let mut admins_creds = Vec::new();
	let mut users_creds = Vec::new();
	let mut temp_users = Vec::new();

	for index in 1..=NB_ADMINS {
		let (utilisateur, creds) = insert_admin(tx, index, cost).await?;
		temp_users.push(utilisateur);
		admins_creds.push(creds);
	}
	for index in 0..NB_USERS {
		let (utilisateur, creds) = insert_user(tx, index, cost).await?;
		temp_users.push(utilisateur);
		users_creds.push(creds);
	}
	Ok((admins_creds, users_creds, temp_users))
}

/// Crée un lot de plantes.
/// @tx transaction ouverte
async fn create_plants_tx(tx: &mut Transaction<'_, Postgres>) -> Result<Vec<TempPlant>, AppError> {
	let mut rng = rand::thread_rng();
	let mut temp_plants = Vec::new();
	for indice in 0..NB_PLANTS {
		let name = PLANT_NAMES[indice as usize % PLANT_NAMES.len()].to_string();
		let price = BigDecimal::from(rng.gen_range(5..51));
		let stock = rng.gen_range(5..31);
		temp_plants.push(insert_plant(tx, name, price, stock).await?);
	}
	Ok(temp_plants)
}

/// Crée des commandes aléatoires.
/// @tx transaction @users users @plants plantes
async fn create_orders_tx(tx: &mut Transaction<'_, Postgres>, users: Vec<TempUser>, mut plants: Vec<TempPlant>) -> Result<(), AppError> {
	let mut rng = rand::thread_rng();
	let statuses = ["pending","confirmed","shipped","delivered"];

	for utilisateur in users {
		let count_orders = rng.gen_range(0..=MAX_ORDERS_PER_USER);
		for _ in 0..count_orders {
			let status = statuses.choose(&mut rng).unwrap().to_string();
			let commande = sqlx::query!("INSERT INTO orders (user_id, total, status) VALUES ($1,0,$2) RETURNING id", utilisateur.id, status)
				.fetch_one(&mut *tx).await?;
			let mut total = BigDecimal::from(0);
			let items = rng.gen_range(1..=3);

			for _ in 0..items {
				if let Some(plante) = plants.choose_mut(&mut rng) {
					if plante.stock > 0 {
						let qty = rng.gen_range(1..=std::cmp::min(5, plante.stock));
						total += add_item_and_decrement_stock(tx, commande.id, plante, qty).await?;
					}
				}
			}
			update_order_total(tx, commande.id, total).await?;
		}
	}
	Ok(())
}

//
// # Écriture des identifiants
//

/// Écrit un fichier users.txt avec les identifiants.
/// @admins admins @users users
fn write_users_file(admins: Vec<(String, String)>, users: Vec<(String, String)>) -> Result<(), AppError> {
	let mut fichier = File::create("users.txt").map_err(|_| AppError::Internal)?;
	let mut contenu = String::from("Administrateurs :\n\n");
	for (email, password) in admins { contenu.push_str(&format!("{} {}\n", email, password)); }
	contenu.push_str("\nUtilisateurs :\n\n");
	for (email, password) in users { contenu.push_str(&format!("{} {}\n", email, password)); }
	fichier.write_all(contenu.as_bytes()).map_err(|_| AppError::Internal)?;
	Ok(())
}

//
// # Main seed
//

/// Point d’entrée du seed (appelé depuis src/bin/seed.rs).
/// Ouvre une transaction globale et applique les opérations en lot.
pub async fn run_seed() -> Result<(), AppError> {
	dotenv().ok();
	let database_url = env::var("DATABASE_URL").map_err(|_| AppError::Internal)?;
	let bcrypt_cost = bcrypt_cost_for_seed();

	let pool: Pool<Postgres> = PgPoolOptions::new().max_connections(10).connect(&database_url).await?;
	let mut tx = pool.begin().await?;

	reset_db_tx(&mut tx).await?;
	let plantes = create_plants_tx(&mut tx).await?;
	let (admins_creds, users_creds, utilisateurs) = create_users_tx(&mut tx, bcrypt_cost).await?;
	create_orders_tx(&mut tx, utilisateurs, plantes).await?;
	tx.commit().await?;

	write_users_file(admins_creds, users_creds)?;
	Ok(())
}
