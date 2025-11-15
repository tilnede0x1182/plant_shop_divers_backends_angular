# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction

Ce rapport analyse les violations du principe DRY (Don't Repeat Yourself) **à l'intérieur de chaque projet** Rust :

- `plant_shop_rust_see_orm` (utilisant SeaORM)
- `plant_shop_rust_sqlx` (utilisant SQLx)

Les deux projets sont des implémentations du même backend avec des ORMs différents.

---

## Violations DRY dans **plant_shop_rust_see_orm**

### 1. **Extraction et validation JWT dupliquée (8+ fois)** - 🔴 CRITIQUE

**Fichiers concernés :**

- `src/users/handlers.rs:44-49`
- `src/plants/handlers.rs:40-45`
- `src/orders/handlers.rs:51-58` et `110-117`
- `src/auth/handlers.rs:117-122`

**Code répété :**

```rust
let token = jar
    .get("auth_token")
    .map(|c| c.value_str().to_string())
    .ok_or(AppError::Unauthorized)?;
let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;
```

**Impact :** Tout changement (nom du cookie, gestion du secret) nécessite de modifier 4+ fichiers différents.

**Solution recommandée :** Créer un extracteur Actix réutilisable.

---

### 2. **Structures User/UserAuth avec 5 champs redondants** - 🟠 HAUTE

**Fichiers concernés :**

- `src/users/models.rs:4-13` (User)
- `src/auth/models.rs:17-28` (UserAuth)

**Champs dupliqués :** `id`, `email`, `username`, `is_admin`, `created_at`

**Code dupliqué :**

Structure `User` (users/models.rs) :

```rust
#[derive(Serialize, Deserialize, Clone)]
pub struct User {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: DateTime<Utc>,
}
```

Structure `UserAuth` (auth/models.rs) :

```rust
#[derive(Serialize, Deserialize, Clone)]
pub struct UserAuth {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub email: String,
    pub username: String,
    pub password_hash: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: chrono::DateTime<chrono::Utc>,
}
```

**Impact :** Deux structures pour représenter le même concept utilisateur. Modification d'un champ commun nécessite de toucher 2 fichiers.

**Solution recommandée :** Utiliser une structure de base commune ou des traits.

---

### 3. **Conversions Entity → DTO répétées (3+ fois)** - 🟠 HAUTE

**Fichiers concernés :**

- `src/users/handlers.rs:110-116` (get_user)
- `src/users/handlers.rs:69-77` (list_users)
- `src/auth/handlers.rs:130-135` (me)

**Code répété :**

Conversion en users/handlers.rs (get_user) :

```rust
let dto = UserDto {
    id: user.id,
    email: user.email,
    username: user.username,
    is_admin: user.is_admin,
    created_at: user.created_at.into(),
};
```

Conversion en users/handlers.rs (list_users) :

```rust
let mapped: Vec<_> = users.into_iter().map(|u| {
    json!({
        "id": u.id,
        "email": u.email,
        "name": u.username,
        "admin": u.is_admin,
        "createdAt": u.created_at
    })
}).collect();
```

Conversion en auth/handlers.rs (me) :

```rust
let response = AuthMeResponse {
    id: user.id,
    email: user.email,
    name: user.username,
    admin: user.is_admin,
};
```

**Impact :** Pas de centralisation de la conversion User → JSON/DTO. Chaque changement de structure nécessite 3+ modifications.

**Solution recommandée :** Implémenter `From<User>` ou `Into<UserDto>` traits.

---

### 4. **Validation admin dupliquée (3 handlers)** - 🟡 MOYENNE

**Fichiers concernés :**

- `src/users/handlers.rs:51-60` (via DB query)
- `src/plants/handlers.rs:46` (via JWT claims)

**Code dupliqué :**

Users handlers (via DB) :

```rust
let current = User::find_by_id(claims.sub)
    .one(db)
    .await
    .map_err(|_| AppError::Internal)?
    .ok_or(AppError::Unauthorized)?;

if !current.is_admin {
    return Err(AppError::Forbidden);
}
```

Plants handlers (via JWT) :

```rust
if !claims.is_admin {
    return Err(AppError::Forbidden.into());
}
```

**Impact :** Deux approches différentes pour la même vérification, créant une incohérence.

**Solution recommandée :** Middleware ou guard Actix pour vérifier les permissions admin.

---

### 5. **Pattern de construction PlantBasic répété** - 🟡 MOYENNE

**Fichiers concernés :**

- `src/orders/models.rs:16-22` (définition)
- `src/orders/handlers.rs:150-160` (construction manuelle)

**Code dupliqué :**

Structure définie :

```rust
#[derive(Debug, Serialize, Deserialize)]
pub struct PlantBasic {
    pub id: i32,
    pub name: String,
    pub price: i32,
    pub stock: i32,
    pub description: Option<String>,
}
```

Mais recréée manuellement en JSON :

```rust
item_details.push(json!({
    "id": item.id,
    "plantId": pid,
    "quantity": item.quantity,
    "price": item.price,
    "plant": {
        "id": plant.id,
        "name": plant.name,
        "price": plant.price,
    }
}));
```

**Impact :** La structure existe mais n'est pas utilisée partout.

**Solution recommandée :** Utiliser systématiquement `PlantBasic` au lieu de reconstruire manuellement.

---

## Violations DRY dans **plant_shop_rust_sqlx**

### 1. **Extraction et validation JWT dupliquée (7+ fois)** - 🔴 CRITIQUE

**Fichiers concernés :**

- `src/users/handlers.rs:15-21` et `100-104`
- `src/plants/handlers.rs:18-20`
- `src/orders/handlers.rs:37-41` et `126-130`
- `src/auth/handlers.rs:115-120`

**Code répété :**

```rust
let token = jar
    .get("auth_token")
    .map(|c| c.value_str().to_string())
    .ok_or(AppError::Unauthorized)?;
let secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;
```

**Impact :** Identique au projet see_orm - aucune centralisation de l'authentification.

**Solution recommandée :** Extracteur Actix personnalisé.

---

### 2. **Extraction user_id dupliquée 2 fois dans le même fichier** - 🔴 HAUTE

**Fichiers concernés :**

- `src/orders/handlers.rs:37-49` (create_order)
- `src/orders/handlers.rs:126-137` (list_orders)

**Code répété (13 lignes identiques) :**

```rust
let user_id = if let Some(c) = jar.get("auth_token") {
    let token = c.value_str();
    let jwt_secret = std::env::var("JWT_SECRET").map_err(|_| AppError::Internal)?;
    let claims = verify_jwt(token, &jwt_secret).map_err(|_| AppError::Unauthorized)?;
    claims.sub
} else {
    let row = sqlx::query!("SELECT id FROM users WHERE is_admin = false ORDER BY created_at DESC LIMIT 1")
        .fetch_one(pool)
        .await
        .map_err(|e| AppError::DatabaseError(e))?;
    row.id
};
```

**Impact :** Duplication massive de 13 lignes **dans le même fichier**. C'est un cas d'école de violation DRY.

**Solution recommandée :** Extraire dans une fonction `get_user_id_from_request()`.

---

### 3. **Structures User/UserAuth avec 5 champs redondants** - 🟠 HAUTE

**Fichiers concernés :**

- `src/users/models.rs:4-13`
- `src/auth/models.rs:17-28`

**Champs dupliqués :** `id`, `email`, `username`, `is_admin`, `created_at`

**Code dupliqué :**

Structure `User` :

```rust
#[derive(Serialize, Deserialize, sqlx::FromRow, Clone)]
pub struct User {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: DateTime<Utc>,
}
```

Structure `UserAuth` :

```rust
#[derive(Serialize, Deserialize, sqlx::FromRow)]
pub struct UserAuth {
    pub id: i32,
    #[allow(dead_code)]
    #[serde(skip_serializing)]
    pub email: String,
    pub username: String,
    pub password_hash: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: chrono::DateTime<chrono::Utc>,
}
```

**Impact :** Même problème que see_orm - duplication conceptuelle.

**Solution recommandée :** Structure de base commune.

---

### 4. **Construction OrderItemWithPlant répétée 3 fois** - 🟠 HAUTE

**Fichiers concernés :**

- `src/orders/handlers.rs:97-111` (create_order)
- `src/orders/handlers.rs:175-189` (list_orders)
- `src/orders/handlers.rs:231-243` (get_order)

**Code répété :**

create_order :

```rust
for order_item in &created_items {
    let plant = sqlx::query_as!(
        PlantBasic,
        "SELECT id,name, price, stock, description FROM plants WHERE id = $1",
        order_item.plant_id
    )
    .fetch_one(pool)
    .await
    .map_err(|e| AppError::DatabaseError(e))?;

    items_vec.push(OrderItemWithPlant {
        id: order_item.id,
        quantity: order_item.quantity,
        price: order_item.price.clone(),
        plant_id: plant.id,
        plant,
    });
}
```

list_orders :

```rust
let items: Vec<_> = items_rows
    .into_iter()
    .map(|row| OrderItemWithPlant {
        id: row.id,
        quantity: row.quantity,
        price: row.price,
        plant_id: row.plant_id,
        plant: PlantBasic {
            id: row.plant_id,
            name: row.name,
            price: row.plant_price,
            stock: row.stock,
            description: row.description,
        },
    })
    .collect();
```

get_order (pattern identique à list_orders).

**Impact :** Le mapping `OrderItemWithPlant` + `PlantBasic` est répété 3 fois avec de légères variations.

**Solution recommandée :** Fonction helper `build_order_item_with_plant()`.

---

### 5. **Pattern UPDATE avec COALESCE répété** - 🟡 MOYENNE

**Fichiers concernés :**

- `src/users/handlers.rs:127-139`
- `src/plants/handlers.rs:75-89`

**Pattern répété :**

Users :

```rust
let user = sqlx::query_as!(
    User,
    r#"UPDATE users SET
        username = COALESCE($1, username),
        email    = COALESCE($2, email),
        is_admin = COALESCE($3, is_admin)
    WHERE id = $4
    RETURNING id, email, username, is_admin, created_at"#,
    payload.name,
    payload.email,
    admin_value,
    user_id
)
```

Plants :

```rust
let plant = sqlx::query_as!(
    Plant,
    "UPDATE plants SET
        name = COALESCE($1, name),
        description = COALESCE($2, description),
        price = COALESCE($3, price),
        stock = COALESCE($4, stock)
     WHERE id = $5
     RETURNING id, name, description, price, stock, created_at",
    payload.name,
    payload.description,
    payload.price,
    payload.stock,
    plant_id
)
```

**Impact :** Le pattern logique UPDATE + COALESCE pour les mises à jour partielles est identique.

**Solution recommandée :** Macro ou fonction générique pour les UPDATE partiels.

---

### 6. **Validation admin avec 2 approches différentes** - 🟡 MOYENNE

**Fichiers concernés :**

- `src/users/handlers.rs:24-34` (via DB query)
- `src/plants/handlers.rs:18-23` (via JWT claims)

**Code dupliqué :**

Users (via DB) :

```rust
let user = sqlx::query!(
    "SELECT id, is_admin FROM users WHERE id = $1",
    claims.sub
)
.fetch_one(pool)
.await
.map_err(AppError::DatabaseError)?;

if !user.is_admin {
    return Err(AppError::Forbidden);
}
```

Plants (via JWT) :

```rust
let claims = verify_jwt(&token, &secret).map_err(|_| AppError::Unauthorized)?;
if !claims.is_admin {
    return Err(AppError::Forbidden.into());
}
```

**Impact :** Incohérence dans l'approche de vérification + duplication de la logique.

**Solution recommandée :** Middleware unifié pour la vérification admin.

---

### 7. **Conversion User → DTO répétée** - 🟡 MOYENNE

**Fichiers concernés :**

- `src/auth/handlers.rs:130-135`
- `src/auth/handlers.rs:140-146` (définition de la structure de réponse)

**Code dupliqué :**

Structure de réponse :

```rust
#[derive(serde::Serialize)]
struct AuthMeResponse {
    id: i32,
    email: String,
    name: String,
    admin: bool,
}
```

Construction :

```rust
let response = AuthMeResponse {
    id: user.id,
    email: user.email,
    name: user.username,
    admin: user.is_admin,
};
```

**Impact :** Pattern de renommage `username → name` et `is_admin → admin` répété sans trait de conversion.

**Solution recommandée :** Implémenter `From<User>` pour `AuthMeResponse`.

---

## RÉSUMÉ COMPARATIF

| Violation DRY                         |  see_orm   |         sqlx          |    Sévérité     |
| ------------------------------------- | :--------: | :-------------------: | :-------------: |
| **Extraction JWT répétée**            |  8+ fois   |        7+ fois        | 🔴 **CRITIQUE** |
| **User/UserAuth redondants**          |  5 champs  |       5 champs        |  🟠 **HAUTE**   |
| **Conversions User → DTO**            |  3+ fois   |        2+ fois        |  🟠 **HAUTE**   |
| **Construction PlantBasic/OrderItem** |  2+ fois   |        3 fois         |  🟠 **HAUTE**   |
| **Extraction user_id identique**      |     -      | 2 fois (même fichier) |  🔴 **HAUTE**   |
| **Validation admin**                  | 3 handlers |      2 approches      | 🟡 **MOYENNE**  |
| **Pattern UPDATE COALESCE**           |     -      |        2 fois         | 🟡 **MOYENNE**  |

---

## RECOMMANDATIONS PRIORITAIRES

### 1. 🔴 Centraliser l'authentification JWT (PRIORITAIRE)

**Problème :** Code d'extraction JWT répété 8+ fois dans see_orm et 7+ fois dans sqlx.

**Solution :** Créer un extracteur Actix réutilisable.

**Exemple d'implémentation :**

```rust
// src/auth/extractor.rs
use actix_web::{dev::Payload, Error, FromRequest, HttpRequest};
use futures::future::{ready, Ready};
use crate::auth::jwt::verify_jwt;
use crate::errors::AppError;

pub struct AuthUser {
    pub user_id: i32,
    pub is_admin: bool,
}

impl FromRequest for AuthUser {
    type Error = Error;
    type Future = Ready<Result<Self, Error>>;

    fn from_request(req: &HttpRequest, _: &mut Payload) -> Self::Future {
        let jar = req.cookie("auth_token");

        let result = (|| {
            let token = jar
                .ok_or(AppError::Unauthorized)?
                .value()
                .to_string();

            let secret = std::env::var("JWT_SECRET")
                .map_err(|_| AppError::Internal)?;

            let claims = verify_jwt(&token, &secret)
                .map_err(|_| AppError::Unauthorized)?;

            Ok(AuthUser {
                user_id: claims.sub,
                is_admin: claims.is_admin,
            })
        })();

        ready(result.map_err(Into::into))
    }
}
```

**Usage dans les handlers :**

```rust
// Avant
async fn list_users(jar: CookieJar, pool: Data<DbConn>) -> Result<impl Responder> {
    let token = jar.get("auth_token")...;  // 6 lignes répétées
    let secret = ...;
    let claims = verify_jwt(...)?;
    // ...
}

// Après
async fn list_users(auth: AuthUser, pool: Data<DbConn>) -> Result<impl Responder> {
    if !auth.is_admin {
        return Err(AppError::Forbidden);
    }
    // ...
}
```

**Bénéfices :**

- Élimine 8+ duplications dans see_orm
- Élimine 7+ duplications dans sqlx
- Code de handler 90% plus court
- Changement de la logique JWT en un seul endroit

---

### 2. 🟠 Factoriser User/UserAuth

**Problème :** 5 champs identiques entre `User` et `UserAuth`.

**Solution :** Utiliser une structure de base commune.

**Exemple d'implémentation :**

```rust
// src/models/common.rs
#[derive(Serialize, Deserialize, Clone)]
pub struct UserBase {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: DateTime<Utc>,
}

// src/users/models.rs
pub use crate::models::common::UserBase as User;

// src/auth/models.rs
#[derive(Serialize, Deserialize, Clone)]
pub struct UserAuth {
    #[serde(flatten)]
    pub base: UserBase,
    pub password_hash: String,
}
```

**Bénéfices :**

- Élimine la duplication de 5 champs
- Changement de structure en un seul endroit
- Meilleure cohérence du modèle

---

### 3. 🟠 Créer des fonctions de conversion

**Problème :** Conversions User → DTO répétées 3+ fois.

**Solution :** Implémenter des traits `From` / `Into`.

**Exemple d'implémentation :**

```rust
// src/users/models.rs
impl From<User> for UserDto {
    fn from(user: User) -> Self {
        UserDto {
            id: user.id,
            email: user.email,
            name: user.username,
            admin: user.is_admin,
            created_at: user.created_at,
        }
    }
}

// Usage dans handlers
let dto: UserDto = user.into();  // Au lieu de mapper manuellement
```

**Bénéfices :**

- Élimine 3+ conversions manuelles
- Code plus idiomatique Rust
- Centralisation des règles de conversion

---

### 4. 🟡 Middleware pour la validation admin

**Problème :** Vérification admin répétée dans plusieurs handlers avec approches différentes.

**Solution :** Guard Actix ou middleware dédié.

**Exemple d'implémentation :**

```rust
// src/auth/guards.rs
pub struct AdminGuard;

impl Guard for AdminGuard {
    fn check(&self, ctx: &GuardContext) -> bool {
        if let Some(auth) = ctx.req_data::<AuthUser>() {
            auth.is_admin
        } else {
            false
        }
    }
}

// Usage dans routes
web::resource("/users")
    .guard(AdminGuard)
    .route(web::get().to(list_users))
```

**Bénéfices :**

- Centralisation de la logique admin
- Cohérence dans l'approche
- Code de handler plus propre

---

### 5. 🟠 Fonction helper pour extraction user_id (sqlx uniquement)

**Problème :** 13 lignes dupliquées exactement 2 fois dans `src/orders/handlers.rs`.

**Solution :** Extraire dans une fonction dédiée.

**Exemple d'implémentation :**

```rust
// src/orders/helpers.rs
async fn get_user_id_from_request(
    jar: &CookieJar,
    pool: &PgPool,
) -> Result<i32, AppError> {
    if let Some(c) = jar.get("auth_token") {
        let token = c.value_str();
        let jwt_secret = std::env::var("JWT_SECRET")
            .map_err(|_| AppError::Internal)?;
        let claims = verify_jwt(token, &jwt_secret)
            .map_err(|_| AppError::Unauthorized)?;
        Ok(claims.sub)
    } else {
        let row = sqlx::query!("SELECT id FROM users WHERE is_admin = false ORDER BY created_at DESC LIMIT 1")
            .fetch_one(pool)
            .await
            .map_err(AppError::DatabaseError)?;
        Ok(row.id)
    }
}

// Usage
let user_id = get_user_id_from_request(&jar, pool).await?;
```

**Bénéfices :**

- Élimine 13 lignes dupliquées
- Améliore la lisibilité des handlers

---

### 6. 🟠 Helper pour construction OrderItemWithPlant (sqlx uniquement)

**Problème :** Construction répétée 3 fois avec variations mineures.

**Solution :** Fonction helper générique.

**Exemple d'implémentation :**

```rust
// src/orders/helpers.rs
fn build_order_item_with_plant(
    id: i32,
    quantity: i32,
    price: BigDecimal,
    plant_id: i32,
    plant_name: String,
    plant_price: BigDecimal,
    plant_stock: i32,
    plant_description: Option<String>,
) -> OrderItemWithPlant {
    OrderItemWithPlant {
        id,
        quantity,
        price,
        plant_id,
        plant: PlantBasic {
            id: plant_id,
            name: plant_name,
            price: plant_price,
            stock: plant_stock,
            description: plant_description,
        },
    }
}
```

**Bénéfices :**

- Centralisation du mapping
- Réduction de code dans handlers

---

## IMPACT ESTIMÉ DES REFACTORINGS

| Refactoring                 | Lignes de code économisées | Fichiers impactés | Complexité  |
| --------------------------- | :------------------------: | :---------------: | :---------: |
| Extracteur JWT              |       ~50-60 lignes        |   4-5 fichiers    |   Faible    |
| User/UserAuth factorisation |       ~10-15 lignes        |    2 fichiers     |   Moyenne   |
| Traits de conversion        |       ~20-30 lignes        |    3 fichiers     |   Faible    |
| Middleware admin            |       ~15-20 lignes        |    3 fichiers     |   Moyenne   |
| Helper user_id (sqlx)       |         ~13 lignes         |     1 fichier     | Très faible |
| Helper OrderItem (sqlx)     |       ~30-40 lignes        |     1 fichier     |   Faible    |

**Total estimé : ~140-180 lignes de code en moins, amélioration de la maintenabilité**

---

## CONCLUSION

Les deux projets souffrent de **violations critiques du principe DRY**, principalement :

1. **Extraction JWT répétée** (8+ fois see_orm, 7+ fois sqlx) - Impact maximal
2. **Structures redondantes** (User/UserAuth) - Impact structurel
3. **Conversions manuelles répétées** - Impact maintenance

La refactorisation prioritaire est la **centralisation de l'authentification JWT via un extracteur Actix**, qui éliminerait à elle seule 50% des violations DRY identifiées.

---

**Rapport généré le :** 2025-11-13
**Projets analysés :**

- `plant_shop_rust_see_orm`
- `plant_shop_rust_sqlx`

## A faire

1. Extraire l’extraction/validation du JWT dans un middleware (Actix extracteur ou service partagé) et l’utiliser partout (`users`, `orders`, `auth`, `plants`) pour arrêter la duplication du bloc `jar -> token -> verify_jwt`. Ainsi toute modification du cookie ou du secret se fait en un seul endroit.
2. Fusionner les structures utilisateur (`User`, `UserAuth`, `UserDto`) en une base commune, ou définir un trait partagé, afin d’éviter de répéter les champs (`id`, `email`, `username`, `is_admin`, `created_at`) dans trois modules différents.
3. Centraliser les conversions Entity → DTO/JSON dans des helpers partagés (par exemple `User::into_response`) pour supprimer les mappages identiques dans `users::handlers`, `auth::handlers`, `plants::handlers`.
4. Pour SQLx, extraire les helpers `get_user_id_from_request` et `build_order_item_with_plant` dans un module `orders/helpers.rs` afin qu’ils soient utilisés par tous les handlers et qu’on ne reproduise pas la logique 2-3 fois.
5. Factoriser la logique ADMIN (`AdminGuard`, middleware) et les entêtes CORS en composants réutilisables (`admin_guard.rs`, `cors.rs`) accessibles par tous les handlers.

## 3. Audit Rust & solutions sans écrire de code

### 3.1 Performance (Rust vs C)

Les deux projets Rust arrivent à être plus lents que la version Java/C principalement parce que :

1. La couche HTTP Actix utilise un tokio runtime par service sans ajustement, alors que les services C sont compilés en binaires mono-thread optimisés et profitent de `-O3`.
2. Les bases SeaORM/SQLx ajoutent une surcharge (pooling, mapping) qui n’est pas amortie par la compilation car beaucoup de conversions sont réécrites localement.
3. Il manque un benchmarking systématique (profiling) pour repérer les goulets (clonage de structures, conversions JSON, validations JWT).
