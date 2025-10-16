use sqlx::PgPool;
use rand::Rng;
use std::fs::File;
use std::io::Write;
use std::path::Path;

// Constantes
const NB_ADMINS: u32 = 3;
const NB_USERS: u32 = 20;
const NB_PLANTS: u32 = 50;
const MAX_ORDERS_PER_USER: u32 = 7;

// Noms de plantes
const PLANT_NAMES: [&str; 44] = [
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

// Structure pour les utilisateurs
struct User {
    id: u32,
    email: String,
    password: String,
    admin: bool,
    name: String,
}

// Structure pour les plantes
struct Plant {
    id: u32,
    name: String,
    price: f64,
    description: String,
    stock: u32,
}

// Structure pour les commandes
struct Order {
    id: u32,
    user_id: u32,
    total_price: f64,
    status: String,
}

// Structure pour les items de commande
struct OrderItem {
    id: u32,
    order_id: u32,
    plant_id: u32,
    quantity: u32,
}

// Fonction pour créer les admins
async fn create_admins(pool: &PgPool) -> Result<Vec<User>, sqlx::Error> {
    let mut admins = Vec::new();
    for idx in 0..NB_ADMINS {
        let email = format!("admin{}@planteshop.com", idx + 1);
        let password = "password".to_string();
        let admin = User {
            id: 0,
            email,
            password,
            admin: true,
            name: format!("Admin {}", idx + 1),
        };
        sqlx::query!("INSERT INTO users (email, password, admin, name) VALUES ($1, $2, $3, $4) RETURNING *",
            admin.email, admin.password, admin.admin, admin.name)
            .fetch_one(pool)
            .await?
            .into();
        admins.push(admin);
    }
    Ok(admins)
}

// Fonction pour créer les utilisateurs
async fn create_users(pool: &PgPool) -> Result<Vec<User>, sqlx::Error> {
    let mut users = Vec::new();
    for _ in 0..NB_USERS {
        let email = format!("user{}@example.com", rand::thread_rng().gen::<u32>());
        let password = format!("password{}", rand::thread_rng().gen::<u32>());
        let user = User {
            id: 0,
            email,
            password,
            admin: false,
            name: format!("User {}", rand::thread_rng().gen::<u32>()),
        };
        sqlx::query!("INSERT INTO users (email, password, admin, name) VALUES ($1, $2, $3, $4) RETURNING *",
            user.email, user.password, user.admin, user.name)
            .fetch_one(pool)
            .await?
            .into();
        users.push(user);
    }
    Ok(users)
}

// Fonction pour créer les plantes
async fn create_plants(pool: &PgPool) -> Result<Vec<Plant>, sqlx::Error> {
    let mut plants = Vec::new();
    for idx in 0..NB_PLANTS {
        let name = PLANT_NAMES[idx as usize % PLANT_NAMES.len()];
        let price = rand::thread_rng().gen::<f64>() * 100.0;
        let description = format!("Description de la plante {}", idx);
        let stock = rand::thread_rng().gen::<u32>() % 100;
        let plant = Plant {
            id: 0,
            name: name.to_string(),
            price,
            description,
            stock,
        };
        sqlx::query!("INSERT INTO plants (name, price, description, stock) VALUES ($1, $2, $3, $4) RETURNING *",
            plant.name, plant.price, plant.description, plant.stock)
            .fetch_one(pool)
            .await?
            .into();
        plants.push(plant);
    }
    Ok(plants)
}

// Fonction pour créer les commandes
async fn create_orders(pool: &PgPool, plants: Vec<Plant>) -> Result<(), sqlx::Error> {
    for user in sqlx::query!("SELECT * FROM users").fetch_all(pool).await? {
        let user_id = user.id;
        let number_of_orders = rand::thread_rng().gen::<u32>() % MAX_ORDERS_PER_USER;
        for _ in 0..number_of_orders {
            let order = Order {
                id: 0,
                user_id,
                total_price: 0.0,
                status: "pending".to_string(),
            };
            sqlx::query!("INSERT INTO orders (user_id, total_price, status) VALUES ($1, $2, $3) RETURNING *",
                order.user_id, order.total_price, order.status)
                .fetch_one(pool)
                .await?
                .into();
            let mut total = 0.0;
            for _ in 0..2 {
                let plant = &plants[rand::thread_rng().gen::<usize>() % plants.len()];
                let quantity = rand::thread_rng().gen::<u32>() % 5;
                let order_item = OrderItem {
                    id: 0,
                    order_id: order.id,
                    plant_id: plant.id,
                    quantity,
                };
                sqlx::query!("INSERT INTO order_items (order_id, plant_id, quantity) VALUES ($1, $2, $3) RETURNING *",
                    order_item.order_id, order_item.plant_id, order_item.quantity)
                    .fetch_one(pool)
                    .await?
                    .into();
                total += plant.price * quantity as f64;
            }
            sqlx::query!("UPDATE orders SET total_price = $1 WHERE id = $2",
                total, order.id)
                .execute(pool)
                .await?;
        }
    }
    Ok(())
}

// Fonction pour écrire le fichier users.txt
fn write_users_file(admins: Vec<User>, users: Vec<User>) -> Result<(), std::io::Error> {
    let mut file = File::create("users.txt")?;
    let mut txt = "Administrateurs :\n\n".to_string();
    for admin in admins {
        txt.push_str(&format!("{} {}\n", admin.email, admin.password));
    }
    txt.push_str("\nUtilisateurs :\n\n");
    for user in users {
        txt.push_str(&format!("{} {}\n", user.email, user.password));
    }
    file.write_all(txt.as_bytes())?;
    Ok(())
}

// Fonction principale
#[tokio::main]
async fn main() -> Result<(), sqlx::Error> {
    let database_url = "postgres://user:password@localhost/database";
    let pool = PgPool::new(&database_url).await?;
    let admins = create_admins(&pool).await?;
    let users = create_users(&pool).await?;
    let plants = create_plants(&pool).await?;
    create_orders(&pool, plants).await?;
    write_users_file(admins, users)?;
    Ok(())
}
