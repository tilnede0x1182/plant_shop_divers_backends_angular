# Rapport d'optimisation de performances (18 novembre 2025)

## Section 1 – Conseils généraux

### 1. Backend SQLx

- **Réduire les clonages BigDecimal et allocations JSON à chaud** : remplacez les conversions `BigDecimal -> i32` et les `serde_json::Value` intermédiaires par des DTO sérialisables une seule fois (déjà amorcé côté orders) et appliquez le même principe aux modules `users` et `plants` pour éviter les copies inutiles dans les hot paths `list_*`.
- **Pipeline des requêtes SQLx** : regroupez les lectures séquentielles (ex. `create_order` qui fait `SELECT` puis `INSERT` par item) en requêtes combinées (`RETURNING` avec agrégats) et utilisez `fetch_all`/`fetch_many` pour limiter les allers-retours réseau.
- **Éviter les conversions string→DateTime dans les DTO** : stockez `DateTime<Utc>` dès la lecture SQLx (via `AT TIME ZONE 'UTC'`) pour supprimer les `.into()` répétées et les allocations associées.
- **Limiter la contention sur le pool** : créez des `PgPool` dédiés aux workloads lourds (ex. `/api/orders`) avec `acquire_timeout` court et batchez les écritures via transactions réutilisables ; cela réduit les latences dues aux locks du pool partagé.
- **Tokio/Poem tuning** : activez un runtime multi-thread (`worker_threads = num_cpus`) et réutilisez les buffers de sérialisation JSON (ex. `BytesMut`) dans les handlers critiques pour limiter la pression mémoire.
- **Préparer la couche de cache** : exposez des traits pour brancher un cache mémoire (plantes populaires, liste des utilisateurs) alimenté par `tokio::sync::RwLock`. Même sans l’implémenter immédiatement, prévoir les interfaces évite un refactor coûteux.
- **Sécurité** : ces optimisations conservent les contrôles centralisés (`AuthSession`, validation DB) et n’introduisent pas d’accès non filtrés ; les transactions restent explicites, aucun raccourci ne compromet l’intégrité ou les permissions.

### 2. Backend SeaORM

- **Recycler les Transactions** : `create_order` ouvre une transaction puis enchaîne de multiples `SELECT`; enchaînez les vérifications via `find_with_related` ou `bulk_insert` pour réduire les context switches et appels réseau.
- **Limiter `serde_json::Value`** : généralisez les DTO typés introduits côté commandes à tous les endpoints pour supprimer les allocations dynamiques.
- **Pré-calculs côté base** : implémentez des vues matérialisées / `SELECT` avec `ROW_NUMBER` pour les historiques et laissez SeaORM mapper ces vues au lieu de recalculer en Rust.
- **Réduire les conversions ActiveModel** : factorisez les mises à jour partielles (helpers) pour toutes les entités afin de supprimer les `into_active_model()` répétés et les clones associés.
- **Stream des gros résultats** : utilisez `Paginator`/`Stream` SeaORM pour les listes volumineuses afin de traiter les lignes par batch et écrire directement la réponse HTTP.
- **Isoler les allocations Argon2** : mutualisez `Argon2::default()` et les buffers via `once_cell::sync::Lazy` pour réduire le coût des opérations login/register.
- **Sécurité** : l’`AdminGuard` continue d’interroger la DB, les transactions restent explicites et aucun raccourci ne supprime les validations d’entrée ou les contrôles de permission.

## Section 2 – Plan d’actions détaillé

### 2.1 Backend SQLx

1. **DTO sérialisables uniques (même contrat JSON que le front)**
   - Créer un module `dto` regroupant `UserResponse`, `PlantResponse` et `OrderSummary`. Implémenter `From` pour les `sqlx::Row` / entités locales.
   - **Important :** chaque DTO doit garder exactement les clés attendues par le front (`id`, `status`, `totalPrice`, `orderItems[].plant`, etc.). On ne change pas le schéma JSON existant ; on ne fait que déplacer la sérialisation hors des handlers. Profiter de `serde_with::DisplayFromStr` pour serialiser BigDecimal sans clone.
2. **Conversion BigDecimal → i32 sans allocation**
   - Ajouter un trait `BigDecimalExt` (module `plants::models`) exposant `as_i32_lossy()` avec `to_i32().unwrap_or_default()`.
   - Utiliser ce trait dans tous les mappings et le stocker dans les DTO pour supprimer `clone()`.
3. **Batching SQL et agrégations**
   - Refactor `orders::handlers::create_order` : regrouper les `SELECT` via `SELECT id, price, stock FROM plants WHERE id = ANY($1)` (une requête) et les `INSERT` via `QueryBuilder` + `RETURNING`.
   - Utiliser `fetch_many` pour récupérer `Order` + `OrderItems` en un seul round-trip.
4. **Pool et runtime tuning**
   - Introduire un `AppState` contenant `pool_fast` (pour listes) et `pool_tx` (transactions longues) + config `PgPoolOptions::new().acquire_timeout`.
   - Configurer `#[tokio::main(flavor = "multi_thread", worker_threads = num_cpus::get())]`.
5. **Buffers JSON réutilisables**
   - Ajouter un middleware ou utilitaire `JsonBuffer` qui réutilise un `BytesMut` (via `tokio_util::BytesCodec`) pour sérialiser les réponses volumineuses.
6. **Interfaces de cache**
   - Définir un trait `PlantCache` et injecter son implémentation via `AddData`. Les handlers `list_plants`/`list_users` consultent ce trait avant la DB.
7. **Tolérance aux plantes supprimées dans `/api/orders`**
   - Implémentation validée côté SQLx : les colonnes `order_item_*` et `plant_*` sont maintenant typées `Option` dans les requêtes `LEFT JOIN`, `map_row_to_item` loggue et filtre les entrées orphelines, et `list_orders` trace toute erreur SQL (`unexpected null`). Résultat : suppression d’une plante ne provoque plus de panic ni de 500, le front reçoit simplement un tableau `orderItems` potentiellement vide. SeaORM doit reproduire exactement ce schéma (DTO optionnels, logs explicites) pour rester DRY et éviter l’effet domino dans l’autre backend.

### 2.2 Backend SeaORM

1. **Transactions et bulk insert**
   - Modifier `orders::handlers::create_order` pour charger toutes les plantes via `find_by_id` batched et insérer les items avec `order_items::Entity::insert_many`.
2. **DTO typés partout (sans casser le contrat JSON)**
   - Étendre les DTO de `orders::models` aux modules `auth`, `users`, `plants`. Supprimer toute construction `json!` **en conservant les mêmes propriétés/casse/forme que l’API existante** afin que le front Angular continue de fonctionner sans modification.
3. **Vues SQL / calculs DB**
   - Créer une vue `orders_with_rank` (migration) utilisant `ROW_NUMBER`. Mapper cette vue avec SeaORM pour `list_orders`.
4. **Helpers Update généralisés**
   - Reprendre `apply_user_updates`/`apply_plant_updates` comme pattern pour `orders` (statut) et futures entités.
5. **Streaming/pagination**
   - Utiliser `Paginator` pour `list_plants`/`list_users` avec `page_size` configurable ; transformer les résultats en stream envoyés via `poem::Body::from_read`.
6. **Argon2 Lazy**
   - Déclarer `static ARGON2: Lazy<Argon2> = Lazy::new(Argon2::default);` partagé entre `login` et `register` pour arrêter les réinstanciations.

Ces plans respectent strictement le DRY (helpers et DTO centralisés) et préparent les deux backends à tirer pleinement parti des capacités de Rust sans sacrifier la lisibilité ni la sécurité.
