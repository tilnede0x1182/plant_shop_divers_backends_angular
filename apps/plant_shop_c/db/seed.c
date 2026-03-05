/* ==============================================================================
   Importations
   ============================================================================== */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <libpq-fe.h>
#include <argon2.h>
#include "seed_data.h"

#define NB_ADMINS 3
#define NB_USERS 20
#define NB_PLANTS 50
#define MAX_ORDERS_PER_USER 7
#define MAX_ITEMS_PER_ORDER 5

/* ==============================================================================
   Données
   ============================================================================== */

/* ==============================================================================
   Fonctions utilitaires
   ============================================================================== */
/* ------------------------------------------------------------------------------
   Génération aléatoire
   ------------------------------------------------------------------------------ */
/**
 * Génère un entier aléatoire dans un intervalle.
 *
 * @param min Borne inférieure incluse
 * @param max Borne supérieure incluse
 * @return Entier aléatoire entre min et max
 */
static int random_int_range(int min, int max) {
	if (min > max) return min;
	return min + rand() % (max - min + 1);
}

/**
 * Sélectionne un élément aléatoire dans un tableau de chaînes.
 *
 * @param string_array Tableau de chaînes
 * @param array_length Nombre d'éléments dans le tableau
 * @return Pointeur vers la chaîne sélectionnée
 */
static const char* pick_random_string(const char* const string_array[], int array_length) {
	return string_array[random_int_range(0, array_length - 1)];
}

/**
 * Génère un sel aléatoire pour le hachage.
 *
 * @param salt Buffer de destination pour le sel
 * @param salt_length Taille du sel en octets
 */
static void generate_salt(uint8_t *salt, size_t salt_length) {
	for (size_t idx = 0; idx < salt_length; idx++) {
		salt[idx] = rand();
	}
}

/**
 * Effectue le hachage Argon2id avec les paramètres définis.
 *
 * @param password Mot de passe en clair
 * @param salt Sel généré aléatoirement
 * @param salt_length Taille du sel
 * @param encoded_hash Buffer de sortie pour le hash encodé
 * @param hash_size Taille du buffer de sortie
 */
static void perform_argon2_hash(const char* password, uint8_t* salt, size_t salt_length,
								 char* encoded_hash, size_t hash_size) {
	size_t hash_length = 32;
	int result = argon2id_hash_encoded(2, 1 << 16, 1, password, strlen(password),
										salt, salt_length, hash_length,
										encoded_hash, hash_size);
	if (result != ARGON2_OK) {
		fprintf(stderr, "Erreur de hachage Argon2\n");
		exit(1);
	}
}

/**
 * Renvoie une chaîne hachée Argon2id encodée.
 *
 * @param password Mot de passe en clair à hacher
 * @return Pointeur vers le hash encodé (buffer statique)
 */
static char* hash_password_argon2(const char* password) {
	static char encoded_hash[128];
	uint8_t salt[16];
	generate_salt(salt, sizeof(salt));
	perform_argon2_hash(password, salt, sizeof(salt), encoded_hash, sizeof(encoded_hash));
	return encoded_hash;
}

/**
 * Charge les variables de connexion depuis le fichier .env.
 *
 * @param url Buffer pour DATABASE_URL
 * @param user Buffer pour DATABASE_USER
 * @param pass Buffer pour DATABASE_PASS
 */
static void read_environment_variables(char* database_url, char* database_user, char* database_password) {
	FILE* file = fopen(".env", "r");
	if (!file) { perror(".env"); exit(1); }
	char line[256];
	while (fgets(line, sizeof line, file)) {
		char* equals_sign = strchr(line, '=');
		if (!equals_sign) continue;
		*equals_sign = '\0';
		char* value_string = equals_sign + 1;
		value_string[strcspn(value_string, "\r\n")] = '\0';
		if (!strcmp(line, "DATABASE_URL")) strcpy(database_url, value_string);
		else if (!strcmp(line, "DATABASE_USER")) strcpy(database_user, value_string);
		else if (!strcmp(line, "DATABASE_PASS")) strcpy(database_password, value_string);
	}
	fclose(file);
}

/* ------------------------------------------------------------------------------
   SQL helpers
   ------------------------------------------------------------------------------ */
/**
 * Exécute une requête SQL sans retour de données.
 *
 * @param database_connection Connexion PostgreSQL
 * @param query Requête SQL à exécuter
 */
static void execute_sql(PGconn* db, const char* query) {
	PGresult* result = PQexec(db, query);
	if (PQresultStatus(result) != PGRES_COMMAND_OK) {
		fprintf(stderr, "Exec failed: %s\nQuery: %s\n", PQerrorMessage(db), query);
	}
	PQclear(result);
}

/**
 * Exécute une requête SQL INSERT RETURNING id.
 *
 * @param db Connexion PostgreSQL
 * @param query Requête SQL paramétrée avec RETURNING id
 * @param param_count Nombre de paramètres
 * @param paramValues Tableau des valeurs de paramètres
 * @return ID de la ligne insérée, ou -1 en cas d'erreur
 */
static int execute_sql_returning_id(PGconn* db, const char* query, int param_count,
							  const char* const* paramValues) {
	PGresult* result = PQexecParams(db, query, param_count, NULL, paramValues, NULL, NULL, 0);
	if (PQresultStatus(result) != PGRES_TUPLES_OK || PQntuples(result) == 0) {
		fprintf(stderr, "Exec returning ID failed: %s\nQuery: %s\n", PQerrorMessage(db), query);
		PQclear(result);
		return -1;
	}
	int id = atoi(PQgetvalue(result, 0, 0));
	PQclear(result);
	return id;
}

/**
 * Insère un utilisateur dans la base de données.
 *
 * @param db Connexion PostgreSQL
 * @param name Nom de l'utilisateur
 * @param email Adresse email
 * @param pwd_hash Hash du mot de passe (Argon2)
 * @param is_admin 1 si administrateur, 0 sinon
 * @return ID de l'utilisateur créé
 */
static int insert_user(PGconn* db, const char* name, const char* email,
						const char* pwd_hash, int is_admin) {
	const char* params[4] = {name, email, pwd_hash, is_admin ? "t" : "f"};
	const char* query_sql = "INSERT INTO users(name,email,password_hash,is_admin) "
					  "VALUES($1,$2,$3,$4) RETURNING id";
	return execute_sql_returning_id(db, query_sql, 4, params);
}

/**
 * Insère une plante dans la base de données.
 *
 * @param db Connexion PostgreSQL
 * @param name Nom de la plante
 * @param desc Description de la plante
 * @param price Prix en centimes
 * @param stock Quantité en stock
 * @return ID de la plante créée
 */
static int insert_plant(PGconn* db, const char* name, const char* desc,
						 int price, int stock) {
	char price_str[12], stock_str[12];
	sprintf(price_str, "%d.00", price);
	sprintf(stock_str, "%d", stock);
	const char* params[4] = {name, desc, price_str, stock_str};
	const char* query_sql = "INSERT INTO plants(name,description,price,stock) "
					  "VALUES($1,$2,$3,$4) RETURNING id";
	return execute_sql_returning_id(db, query_sql, 4, params);
}

/**
 * Insère une commande vide pour un utilisateur.
 *
 * @param db Connexion PostgreSQL
 * @param user_id ID de l'utilisateur
 * @return ID de la commande créée
 */
static int insert_order(PGconn* db, int user_id) {
	char user_id_string[12];
	sprintf(user_id_string, "%d", user_id);
	const char* params[2] = {user_id_string, "0.00"};
	const char* query_sql = "INSERT INTO orders(user_id, total, status) "
					  "VALUES($1, $2, 'pending') RETURNING id";
	return execute_sql_returning_id(db, query_sql, 2, params);
}

/**
 * Insère un article dans une commande.
 *
 * @param db Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param plant_id ID de la plante
 * @param quantity Quantité commandée
 * @param price Prix unitaire
 */
static void insert_order_item(PGconn* db, int order_id, int plant_id,
							   int quantity, int price) {
	char order_id_string[12], plant_id_string[12], quantity_string[12], price_str[12];
	sprintf(order_id_string, "%d", order_id);
	sprintf(plant_id_string, "%d", plant_id);
	sprintf(quantity_string, "%d", quantity);
	sprintf(price_str, "%d.00", price);
	const char* params[4] = {order_id_string, plant_id_string, quantity_string, price_str};
	const char* query_sql = "INSERT INTO order_items(order_id, plant_id, quantity, price) "
					  "VALUES ($1,$2,$3,$4)";
	PGresult* result = PQexecParams(db, query_sql, 4, NULL, params, NULL, NULL, 0);
	PQclear(result);
}

/**
 * Met à jour le total d'une commande.
 *
 * @param db Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param total Nouveau total en centimes
 */
static void update_order_total(PGconn* db, int order_id, int total) {
	char order_id_string[12], total_str[12];
	sprintf(order_id_string, "%d", order_id);
	sprintf(total_str, "%d.00", total);
	const char* params[2] = {total_str, order_id_string};
	PGresult* result = PQexecParams(db, "UPDATE orders SET total=$1 WHERE id=$2",
									 2, NULL, params, NULL, NULL, 0);
	PQclear(result);
}

/* ------------------------------------------------------------------------------
   Seed helpers
   ------------------------------------------------------------------------------ */
/**
 * Établit la connexion à la base de données PostgreSQL.
 *
 * @param url URL de la base de données
 * @param user Nom d'utilisateur
 * @param pass Mot de passe
 * @return Connexion PostgreSQL ou NULL si erreur
 */
static PGconn* connect_database(const char* url, const char* user, const char* pass) {
	const char* keys[] = {"dbname", "user", "password", NULL};
	const char* values[] = {database_url, database_user, database_password, NULL};
	PGconn* db = PQconnectdbParams(keys, values, 0);
	if (PQstatus(db) != CONNECTION_OK) {
		fprintf(stderr, "DB connection error: %s\n", PQerrorMessage(db));
		return NULL;
	}
	printf("✅ Connexion à la base de données réussie.\n");
	return db;
}

/**
 * Crée un administrateur et l'écrit dans le fichier.
 *
 * @param db Connexion PostgreSQL
 * @param index Numéro de l'admin (1-based)
 * @param output_file Fichier de sortie
 */
static void seed_one_admin(PGconn* db, int index, FILE* output_file) {
	const char* first = pick_random_string(FIRST, sizeof(FIRST) / sizeof(char*));
	const char* last  = pick_random_string(LAST,  sizeof(LAST)  / sizeof(char*));
	char name[64], email[64];
	sprintf(name,  "%s %s", first, last);
	sprintf(email, "admin%d@planteshop.com", index);
	insert_user(db, name, email, hash_password_argon2("password"), 1);
	fprintf(output_file, "%s password\n", email);
}

/**
 * Crée tous les administrateurs.
 *
 * @param db Connexion PostgreSQL
 * @param txt Fichier de sortie
 */
static void seed_admins(PGconn* db, FILE* txt) {
	printf("👑 Création des administrateurs...\n");
	for (int idx = 0; idx < NB_ADMINS; idx++) {
		seed_one_admin(db, idx + 1, txt);
	}
}

/**
 * Crée un utilisateur et l'écrit dans le fichier.
 *
 * @param db Connexion PostgreSQL
 * @param txt Fichier de sortie
 * @return ID de l'utilisateur créé
 */
static int seed_one_user(PGconn* db, FILE* txt) {
	const char* first = pick_random_string(FIRST, sizeof(FIRST) / sizeof(char*));
	const char* last  = pick_random_string(LAST,  sizeof(LAST)  / sizeof(char*));
	char email[64], pwd[16], name[64];
	sprintf(email, "%s_%s%d@%s", first, last, random_int_range(20, 99), pick_random_string(EMAIL_DOMAINS, 3));
	sprintf(pwd, "pw%d", random_int_range(100000000, 999999999));
	sprintf(name, "%s %s", first, last);
	int user_id = insert_user(db, name, email, hash_password_argon2(pwd), 0);
	fprintf(txt, "%s %s\n", email, pwd);
	return user_id;
}

/**
 * Crée tous les utilisateurs.
 *
 * @param db Connexion PostgreSQL
 * @param txt Fichier de sortie
 * @param user_ids Tableau de sortie pour les IDs
 */
static void seed_users(PGconn* db, FILE* txt, int* user_ids) {
	printf("👥 Création des utilisateurs...\n");
	for (int idx = 0; idx < NB_USERS; idx++) {
		user_ids[idx] = seed_one_user(db, txt);
	}
}

/**
 * Crée toutes les plantes.
 *
 * @param db Connexion PostgreSQL
 * @param plant_ids Tableau de sortie pour les IDs
 * @param plant_prices Tableau de sortie pour les prix
 * @param plant_stocks Tableau de sortie pour les stocks
 */
static void seed_plants(PGconn* db, int* plant_ids, int* plant_prices, int* plant_stocks) {
	printf("🌱 Création des plantes...\n");
	for (int idx = 0; idx < NB_PLANTS; idx++) {
		plant_prices[idx] = random_int_range(5, 50);
		plant_stocks[idx] = random_int_range(10, 50);
		plant_ids[idx] = insert_plant(db, PLANT_NAMES[idx], "Une belle plante à découvrir.",
									   plant_prices[idx], plant_stocks[idx]);
	}
}

/**
 * Ajoute un article à une commande si stock disponible.
 *
 * @param db Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param plant_ids Tableau des IDs de plantes
 * @param plant_prices Tableau des prix
 * @param plant_stocks Tableau des stocks (modifié)
 * @return Montant ajouté au total
 */
static int add_order_item_if_stock(PGconn* db, int order_id, int* plant_ids,
									int* plant_prices, int* plant_stocks) {
	int plant_index = random_int_range(0, NB_PLANTS - 1);
	if (plant_stocks[plant_index] <= 0) return 0;
	int qty = random_int_range(1, 3);
	if (qty > plant_stocks[plant_index]) qty = plant_stocks[plant_index];
	insert_order_item(db, order_id, plant_ids[plant_index], qty, plant_prices[plant_index]);
	plant_stocks[plant_index] -= qty;
	return plant_prices[plant_index] * qty;
}

/**
 * Crée une commande avec ses articles pour un utilisateur.
 *
 * @param db Connexion PostgreSQL
 * @param user_id ID de l'utilisateur
 * @param plant_ids Tableau des IDs de plantes
 * @param plant_prices Tableau des prix
 * @param plant_stocks Tableau des stocks (modifié)
 * @return 1 si commande créée, 0 sinon
 */
static int seed_one_order(PGconn* db, int user_id, int* plant_ids,
						   int* plant_prices, int* plant_stocks) {
	int order_id = insert_order(db, user_id);
	if (order_id == -1) return 0;
	int order_total = 0;
	int item_count = random_int_range(1, MAX_ITEMS_PER_ORDER);
	for (int item_index = 0; item_index < item_count; item_index++) {
		order_total += add_order_item_if_stock(db, order_id, plant_ids,
												plant_prices, plant_stocks);
	}
	if (order_total > 0) update_order_total(db, order_id, order_total);
	return 1;
}

/**
 * Crée toutes les commandes pour tous les utilisateurs.
 *
 * @param db Connexion PostgreSQL
 * @param user_ids Tableau des IDs utilisateurs
 * @param plant_ids Tableau des IDs de plantes
 * @param plant_prices Tableau des prix
 * @param plant_stocks Tableau des stocks (modifié)
 * @return Nombre total de commandes créées
 */
static int seed_orders(PGconn* db, int* user_ids, int* plant_ids,
						int* plant_prices, int* plant_stocks) {
	printf("🛒 Création des commandes et articles...\n");
	int total_orders = 0;
	for (int user_index = 0; user_index < NB_USERS; user_index++) {
		int order_count = random_int_range(1, MAX_ORDERS_PER_USER);
		for (int order_index = 0; order_index < order_count; order_index++) {
			total_orders += seed_one_order(db, user_ids[user_index], plant_ids,
											plant_prices, plant_stocks);
		}
	}
	return total_orders;
}

/**
 * Seed les données utilisateurs (admins + users).
 *
 * @param db Connexion PostgreSQL
 * @param txt Fichier de sortie
 * @param user_ids Tableau de sortie pour les IDs utilisateurs
 */
static void seed_all_users(PGconn* db, FILE* txt, int* user_ids) {
	fprintf(txt, "Administrateurs :\n\n");
	seed_admins(db, txt);
	fprintf(txt, "\nUtilisateurs :\n\n");
	seed_users(db, txt, user_ids);
}

/**
 * Initialise et exécute le seed de la base de données.
 *
 * @param db Connexion PostgreSQL
 */
static void run_seed(PGconn* db) {
	printf("🧹 Nettoyage des tables...\n");
	execute_sql(db, "TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE");

	FILE* output_file = fopen("users.txt", "w");
	int user_ids[NB_USERS];
	seed_all_users(db, output_file, user_ids);
	fclose(output_file);

	int plant_ids[NB_PLANTS], plant_prices[NB_PLANTS], plant_stocks[NB_PLANTS];
	seed_plants(db, plant_ids, plant_prices, plant_stocks);
	int total = seed_orders(db, user_ids, plant_ids, plant_prices, plant_stocks);
	printf("✅ %d commandes créées.\n", total);
	PQfinish(db);
	puts("🎉 Seed terminée !");
}

/* ==============================================================================
   Main
   ============================================================================== */
/**
 * Point d'entrée du programme de seed.
 *
 * @return 0 si succès, 1 si erreur
 */
int main(void) {
	srand((unsigned)time(NULL));
	char database_url[128] = "", database_user[64] = "", database_password[64] = "";
	read_environment_variables(database_url, database_user, database_password);
	PGconn* db = connect_database(database_url, database_user, database_password);
	if (!db) return 1;
	run_seed(db);
	return 0;
}
