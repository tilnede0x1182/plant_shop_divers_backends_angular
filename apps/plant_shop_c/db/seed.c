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

/* ---------- Utils ---------- */
/**
 * Génère un entier aléatoire dans un intervalle.
 *
 * @param min Borne inférieure incluse
 * @param max Borne supérieure incluse
 * @return Entier aléatoire entre min et max
 */
static int rnd(int min, int max) {
    if (min > max) return min;
    return min + rand() % (max - min + 1);
}

/**
 * Sélectionne un élément aléatoire dans un tableau de chaînes.
 *
 * @param arr Tableau de chaînes
 * @param len Nombre d'éléments dans le tableau
 * @return Pointeur vers la chaîne sélectionnée
 */
static const char* pick(const char* const arr[], int len) { return arr[rnd(0, len - 1)]; }

/**
 * Génère un sel aléatoire pour le hachage.
 * Note: pour une vraie application, utiliser une source cryptographique.
 *
 * @param salt Buffer de destination pour le sel
 * @param len Taille du sel en octets
 */
static void generate_salt(uint8_t *salt, size_t len) {
    for (size_t i = 0; i < len; i++) {
        salt[i] = rand();
    }
}

/** Renvoie une chaîne hachée Argon2id encodée */
static char* hash_argon2(const char* pwd) {
    static char encoded_hash[128];
    uint8_t salt[16];
    generate_salt(salt, sizeof(salt));

    size_t hashlen = 32;
    if (argon2id_hash_encoded(2, 1 << 16, 1,
                              pwd, strlen(pwd),
                              salt, sizeof(salt),
                              hashlen,
                              encoded_hash, sizeof(encoded_hash)) != ARGON2_OK) {
        fprintf(stderr, "Erreur de hachage Argon2\n");
        exit(1);
    }
    return encoded_hash;
}

/**
 * Charge les variables de connexion depuis le fichier .env.
 *
 * @param url Buffer pour DATABASE_URL
 * @param user Buffer pour DATABASE_USER
 * @param pass Buffer pour DATABASE_PASS
 */
static void read_env(char* url, char* user, char* pass) {
    FILE* f = fopen(".env", "r");
    if (!f) { perror(".env"); exit(1); }
    char line[256];
    while (fgets(line, sizeof line, f)) {
        char* eq = strchr(line, '=');
        if (!eq) continue;
        *eq = '\0';
        char* val = eq + 1;
        val[strcspn(val, "\r\n")] = '\0';
        if (!strcmp(line, "DATABASE_URL")) strcpy(url, val);
        else if (!strcmp(line, "DATABASE_USER")) strcpy(user, val);
        else if (!strcmp(line, "DATABASE_PASS")) strcpy(pass, val);
    }
    fclose(f);
}

/* ---------- SQL helpers ---------- */
/**
 * Exécute une requête SQL sans retour de données.
 *
 * @param db Connexion PostgreSQL
 * @param query Requête SQL à exécuter
 */
static void exec(PGconn* db, const char* q) {
    PGresult* r = PQexec(db, q);
    if (PQresultStatus(r) != PGRES_COMMAND_OK) {
        fprintf(stderr, "Exec failed: %s\nQuery: %s\n", PQerrorMessage(db), q);
    }
    PQclear(r);
}

/**
 * Exécute une requête SQL INSERT RETURNING id.
 *
 * @param db Connexion PostgreSQL
 * @param query Requête SQL paramétrée avec RETURNING id
 * @param nParams Nombre de paramètres
 * @param paramValues Tableau des valeurs de paramètres
 * @return ID de la ligne insérée, ou -1 en cas d'erreur
 */
static int exec_returning_id(PGconn* db, const char* query, int nParams, const char* const* paramValues) {
    PGresult* r = PQexecParams(db, query, nParams, NULL, paramValues, NULL, NULL, 0);
    if (PQresultStatus(r) != PGRES_TUPLES_OK || PQntuples(r) == 0) {
        fprintf(stderr, "Exec returning ID failed: %s\nQuery: %s\n", PQerrorMessage(db), query);
        PQclear(r);
        return -1;
    }
    int id = atoi(PQgetvalue(r, 0, 0));
    PQclear(r);
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
static int insert_user(PGconn* db, const char* name, const char* email, const char* pwd_hash, int is_admin) {
    const char* p[4] = {name, email, pwd_hash, is_admin ? "t" : "f"};
    return exec_returning_id(db, "INSERT INTO users(name,email,password_hash,is_admin) VALUES($1,$2,$3,$4) RETURNING id", 4, p);
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
static int insert_plant(PGconn* db, const char* name, const char* desc, int price, int stock) {
    char pr[12], st[12];
    sprintf(pr, "%d.00", price); // Envoi comme NUMERIC
    sprintf(st, "%d", stock);
    const char* p[4] = {name, desc, pr, st};
    return exec_returning_id(db, "INSERT INTO plants(name,description,price,stock) VALUES($1,$2,$3,$4) RETURNING id", 4, p);
}

/**
 * Insère une commande vide pour un utilisateur.
 *
 * @param db Connexion PostgreSQL
 * @param user_id ID de l'utilisateur
 * @return ID de la commande créée
 */
static int insert_order(PGconn* db, int user_id) {
    char uid_str[12];
    sprintf(uid_str, "%d", user_id);
    const char* p[2] = {uid_str, "0.00"};
    return exec_returning_id(db, "INSERT INTO orders(user_id, total, status) VALUES($1, $2, 'pending') RETURNING id", 2, p);
}

/**
 * Insère un article dans une commande.
 *
 * @param db Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param plant_id ID de la plante
 * @param qty Quantité commandée
 * @param price Prix unitaire
 */
static void insert_order_item(PGconn* db, int order_id, int plant_id, int qty, int price) {
    char oid_str[12], pid_str[12], qty_str[12], price_str[12];
    sprintf(oid_str, "%d", order_id);
    sprintf(pid_str, "%d", plant_id);
    sprintf(qty_str, "%d", qty);
    sprintf(price_str, "%d.00", price);
    const char* p[4] = {oid_str, pid_str, qty_str, price_str};
    PGresult* r = PQexecParams(db, "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES ($1,$2,$3,$4)", 4, NULL, p, NULL, NULL, 0);
    PQclear(r);
}

/**
 * Met à jour le total d'une commande.
 *
 * @param db Connexion PostgreSQL
 * @param order_id ID de la commande
 * @param total Nouveau total en centimes
 */
static void update_order_total(PGconn* db, int order_id, int total) {
    char oid_str[12], total_str[12];
    sprintf(oid_str, "%d", order_id);
    sprintf(total_str, "%d.00", total);
    const char* p[2] = {total_str, oid_str};
    PGresult* r = PQexecParams(db, "UPDATE orders SET total=$1 WHERE id=$2", 2, NULL, p, NULL, NULL, 0);
    PQclear(r);
}

/* ---------- Main ---------- */
int main(void) {
    srand((unsigned)time(NULL));

    char url[128] = "", user[64] = "", pass[64] = "";
    read_env(url, user, pass);
    const char* keys[] = {"dbname", "user", "password", NULL};
    const char* vals[] = {url, user, pass, NULL};
    PGconn* db = PQconnectdbParams(keys, vals, 0);
    if (PQstatus(db) != CONNECTION_OK) {
        fprintf(stderr, "DB connection error: %s\n", PQerrorMessage(db));
        return 1;
    }
    printf("✅ Connexion à la base de données réussie.\n");

    printf("🧹 Nettoyage des tables...\n");
    exec(db, "TRUNCATE order_items,orders,plants,users RESTART IDENTITY CASCADE");

    FILE* txt = fopen("users.txt", "w");
    fprintf(txt, "Administrateurs :\n\n");

    /* ---------- Administrateurs ---------- */
    printf("👑 Création des administrateurs...\n");
    for (int i = 0; i < NB_ADMINS; i++) {
        const char* first = pick(FIRST, sizeof(FIRST) / sizeof(char*));
        const char* last  = pick(LAST,  sizeof(LAST)  / sizeof(char*));
        char name[64], email[64];
        sprintf(name,  "%s %s", first, last);
        sprintf(email, "admin%d@planteshop.com", i + 1);

        insert_user(db, name, email, hash_argon2("password"), 1);
        fprintf(txt, "%s password\n", email);
    }

    fprintf(txt, "\nUtilisateurs :\n\n");

    /* ---------- Utilisateurs ---------- */
    printf("👥 Création des utilisateurs...\n");
    int user_ids[NB_USERS];
    for (int i = 0; i < NB_USERS; i++) {
        const char* first = pick(FIRST, sizeof(FIRST) / sizeof(char*));
        const char* last  = pick(LAST,  sizeof(LAST)  / sizeof(char*));
        char email[64], pwd[16], name[64];
        sprintf(email, "%s_%s%d@%s", first, last, rnd(20, 99), pick(EMAIL_DOMAINS, 3));
        sprintf(pwd, "pw%d", rnd(100000000, 999999999));
        sprintf(name, "%s %s", first, last);
        user_ids[i] = insert_user(db, name, email, hash_argon2(pwd), 0);
        fprintf(txt, "%s %s\n", email, pwd);
    }

    /* ---------- Plantes ---------- */
    printf("🌱 Création des plantes...\n");
    int plant_ids[NB_PLANTS], plant_prices[NB_PLANTS], plant_stocks[NB_PLANTS];
    for (int i = 0; i < NB_PLANTS; i++) {
        plant_prices[i] = rnd(5, 50);
        plant_stocks[i] = rnd(10, 50);
        plant_ids[i] = insert_plant(db, PLANT_NAMES[i], "Une belle plante à découvrir.", plant_prices[i], plant_stocks[i]);
    }

    /* ---------- Commandes ---------- */
    printf("🛒 Création des commandes et articles...\n");
    int total_orders = 0;
    for (int i = 0; i < NB_USERS; i++) {
        int num_orders = rnd(1, MAX_ORDERS_PER_USER);
        for (int j = 0; j < num_orders; j++) {
            int order_id = insert_order(db, user_ids[i]);
            if (order_id == -1) continue;
            total_orders++;

            int order_total = 0;
            int num_items = rnd(1, MAX_ITEMS_PER_ORDER);
            for (int k = 0; k < num_items; k++) {
                int plant_idx = rnd(0, NB_PLANTS - 1);
                if (plant_stocks[plant_idx] > 0) {
                    int qty = rnd(1, 3);
                    qty = (qty > plant_stocks[plant_idx]) ? plant_stocks[plant_idx] : qty;

                    insert_order_item(db, order_id, plant_ids[plant_idx], qty, plant_prices[plant_idx]);
                    order_total += plant_prices[plant_idx] * qty;
                    plant_stocks[plant_idx] -= qty;
                }
            }
            if (order_total > 0) {
                update_order_total(db, order_id, order_total);
            }
        }
    }
    printf("✅ %d commandes créées.\n", total_orders);

    fclose(txt);
    PQfinish(db);
    puts("🎉 Seed terminée !");
    return 0;
}
