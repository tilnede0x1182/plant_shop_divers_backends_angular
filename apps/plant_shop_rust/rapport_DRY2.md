# RAPPORT DRY #2 (18 novembre 2025)

Ce rapport dresse l’état actuel de la duplication de code dans les deux backends Rust du projet « plant_shop » après la centralisation récente des extractions JWT.

## 1. plant_shop_rust_sqlx

### ✅ Améliorations réalisées
- **Extraction JWT** : les handlers (`auth`, `users`, `plants`, `orders`) consomment désormais l’extracteur Poem `AuthSession` (`src/auth/session.rs`). Le bloc `CookieJar -> JWT_SECRET -> verify_jwt` ne figure plus qu’à un seul endroit.
- **Orders helpers** : `src/orders/helpers.rs` factorise l’extraction du `user_id` (fallback inclus) et la construction `OrderItemWithPlant`.

### ⚠️ Duplications restantes
1. **Structures utilisateur redondantes** (`src/users/models.rs` vs `src/auth/models.rs`). Les champs `id`, `email`, `username/name`, `is_admin`, `created_at` restent définis à deux endroits.
2. **Conversions User → DTO** (`users::handlers::list_users`, `auth::handlers::me`, `users::handlers::get_user`). Les renommages `username → name`, `is_admin → admin` et la sérialisation `createdAt` sont répétés.
3. **Validation admin** : mélange de vérification via DB (`users::handlers::list_users`) et via claims (`plants::handlers::create_plant`). Le comportement est correct mais la logique n’est pas centralisée (un guard/middleware commun reste conseillé).
4. **Patterns UPDATE partiels** (`users::handlers::update_user` et `plants::handlers::update_plant`). Les blocs SQL `COALESCE` répètent la même structure.

### Priorité DRY
1. Fusionner les structures `User` / `UserAuth` ou introduire un type commun partagé (enum, trait + conversions).
2. Normaliser les conversions DTO (implémenter `From`/`Into` ou helpers dédiés).
3. Créer un guard/middleware admin unique réutilisable.
4. Étudier une abstraction pour les updates partiels (macro, builder ou helper SQL).

---

## 2. plant_shop_rust_see_orm

### ✅ Améliorations réalisées
- **Extraction JWT** : même extracteur Poem (`src/auth/session.rs`) utilisé par `auth::handlers::me`, `users::handlers` protégés, `plants::handlers::create_plant` et `orders::handlers`.
- L’usage de `AuthSession` aligne les deux projets et élimine 8 duplications signalées dans le rapport initial.

### ⚠️ Duplications restantes
1. **Structures utilisateur multiples** (`src/users/models.rs`, `src/auth/handlers.rs` via `UserModel`, etc.). La logique DTO (`AuthMeResponse`, JSON custom pour `list_users`) reste dispersée.
2. **Conversions JSON manuelles** : `users::handlers::list_users` et `orders::handlers::list_orders` construisent manuellement des `serde_json::Value` avec des clés identiques (renommages `name`, `admin`, `orderItems`).
3. **OrderItem / Plant mapping** : lors de `list_orders`, la structure JSON des items est recréée à la main (même pattern que dans `orders::handlers::create_order` côté SQLx avant helper). Un helper commun serait pertinent.
4. **Mises à jour partielles** (`users::handlers::update_user`, `plants::handlers::update_plant`). Les blocs `into_active_model` + affectations répétées pourraient être factorisés (ex : builder dédié ou macro SeaORM).

### Priorité DRY
1. Définir un module commun de DTO (UserDto/PlantBasic/OrderItemDto) partagé entre handlers `auth`/`users`/`orders`.
2. Créer un helper pour sérialiser les commandes et leurs items (évite les `serde_json::json!` identiques).
3. Factoriser les mises à jour partielles via fonctions utilitaires (ex : `apply_updates(&mut ActiveUser, &UpdateUserDto, can_toggle_admin)`).

---

## 3. Synthèse transversale
- Les deux backends partagent désormais la même stratégie DRY pour l’authentification, ce qui réduit considérablement le risque d’erreur lors d’un changement de cookie ou de secret.
- Les duplications restantes concernent principalement **les DTO utilisateurs et les conversions JSON**, **les helpers d’OrderItem**, et **les patterns d’update partiel**. Ces sujets peuvent être traités de manière quasi identique dans les deux projets afin de maintenir la parité.
- Priorité suivante recommandée : introduire un module `dto` commun (UserDto, PlantBasic, OrderItemDto) et des traits `From<Model>` afin d’éliminer les mappages ad-hoc. Ensuite, standardiser les updates partiels.

Ce rapport sera mis à jour après traitement de ces axes.
