# Étude d'isolement des services

## 1. Enseignements tirés de la version Java HTTP

- **Monolithe distribué** (`Structure_en_application_distribuée/plant_shop_java_distribuée`)

  - Un seul `.env` et un unique couple `db/schema.sql` + `db/Seed.java` pour toutes les tables/fixtures (cf. `db/schema.sql` et `db/Seed.java`).
  - Le `Makefile` pilote une base partagée : `make db-migrate` rejoue ce schéma global avant un seed unique.
  - Les services (auth/catalog/order/user) compilent les mêmes utilitaires (`utils/`) et accèdent tous directement aux tables communes `users`, `plants`, `orders`, `order_items`.

- **Version microservices Java HTTP** (`Structure_en_micro_services/plant_shop_java_microservices`)
  - Même `.env` racine, mais chaque service dispose de son propre dossier `db/` (ex. `catalog-service/db/schema.sql`, `order-service/db/schema.sql`), ce qui limite les dépendances croisés.
  - Le `Makefile` expose des cibles par domaine (`db-migrate-users`, `db-migrate-catalog`, `seed-users`, etc.) et l’ordre global (`db-migrate` puis `seed`) n’est qu’un enchaînement de ces blocs isolés.
  - Les seeds utilisent exclusivement la table du service (ex. `catalog-service/db/Seed.java` ne touche que `plants`) et ne supposent pas la présence des autres domaines.

**Conclusion** : la séparation physique (schemas + seeds + cibles Makefile) de la version Java HTTP microservices sert de référence. Pour Quarkus, il suffit de reproduire ce modèle (ce qui est en cours) et de s’assurer que chaque service ne dépend que de ses propres classes ou de `utils/`.

## 2. Cartographie des dépendances actuelles – Quarkus microservices

Les services Quarkus (`Structure_en_micro_services/plant_shop_java_quarkus_microservices`) partagent la librairie `utils/` pour éviter toute duplication. Voici, pour chacun, les classes externes consommées et la décision « à garder dans utils » vs « à rapatrier » :

### AuthService

- **Références externes** :
  - `repositories.UserRepository` et `models.User` (utils/src/repositories & models)
  - `util.ApiMapper`, `util.PasswordUtil`
  - `util.ForwardedIdentityHolder` via `security.Guards`
- **Décision** : ces composants sont réellement transverses (hash password, mapping JSON, lecture utilisateur). Ils restent dans `utils/` pour conserver le DRY.

### CatalogService

- **Références externes** :
  - `repositories.PlantRepository`, `models.Plant`
  - `util.ApiMapper`
  - `util.ForwardedIdentityHolder` (via `security.Guards`)
- **Décision** : garder `PlantRepository` et `Plant` dans `utils`, car les autres services (order, éventuellement gateway) accèdent aux mêmes DTO/mapper. Les contrôleurs catalog n’importent aucun code d’un autre service.

### OrderService

- **Références externes** :
  - `models.Order`, `models.OrderItem`, `models.Plant`
  - `util.ApiMapper`
  - `repositories.PlantRepository` (depuis `utils`)
- **Décision** : `OrderRepository`/`OrderItemRepository` sont locaux au service, mais l’import de `PlantRepository` signifie que ce service lit directement les données « catalog ». Pour atteindre une isolation complète, il faudra remplacer cet accès par un client HTTP (contrat `ApiMapper.PlantLookup` existe déjà) et ne laisser dans `utils` que l’interface de lookup + les DTO.

### UserService

- **Références externes** :
  - `repositories.UserRepository`, `models.User`
  - `util.ApiMapper`, `util.PasswordUtil`
- **Décision** : logique partagée légitime (mapper DTO, hash). Pas de dépendance directe vers un autre service.

### Points transverses

- `utils/src/models` et `utils/src/repositories` servent de socle commun ; seuls les DTO/contrats nécessaires y restent. Lorsque qu’un service n’a besoin que d’une partie d’un repository d’un autre domaine, il faut extraire une interface dédiée (ex. `PlantLookup`) plutôt que dupliquer toute la classe.
- Aucun fichier de service n’importe directement un package `catalog-service.*`, `user-service.*`, etc. La mutualisation passe exclusivement par `utils/`, ce qui respecte DRY tout en évitant les copier-coller.

Ces observations servent de base pour les prochains refactors (extraction d’un client catalog pour OrderService) sans retoucher aux seeds/Bases déjà alignés.

---

# État des lieux & tâches restantes – plant_shop_java_quarkus_microservices

## 1. Objectif général

Transformer `Structure_en_micro_services/plant_shop_java_quarkus_microservices` en véritable architecture microservices Quarkus en s’alignant sur la référence `plant_shop_java_microservices` (Java HTTP) : chaque service doit gérer son schéma SQL, son seed, ses cibles `make`, et n’importer que les briques réellement partagées via `utils/`, tout en conservant un unique `config/.env` global.

## 2. Ce qui est déjà accompli

- **Migrations/Seeds isolés** :
  - Copie des dossiers `db/` par service (catalog, user, order) depuis la version Micronaut/HTTP.
  - `Makefile` expose désormais `db-migrate-user`, `db-migrate-catalog`, `db-migrate-order`, ainsi que `seed-user`, `seed-catalog`, `seed-order` puis `seed` (enchaînement). Plus de migration implicite dans les seeds.
  - `make seed` rejoue bien les trois seeds indépendants ; `order-service` seed crée sa table `plant_stock` locale.
- **Seuls `.env` racine** : `config/.env` contient les ports + triplet `DATABASE_URL/USER/PASS` pour l’ensemble (pas de duplication `.env` par service, conformément aux contraintes).
- **Étude comparative & dépendances** : `Etude.txt` résume les différences Monolithe vs Microservices et cartographie pour chaque service les classes partagées via `utils/`.
- **Ignorés/copies** : `.gitignore` couvre `.quarkus/` et `bin/maven-workspace/`. Les dossiers `db/` spécifiques sont déjà versionnés (comme dans Micronaut).

## 3. Points restants / TODO pour le prochain LLM

1. **OrderService encore couplé à Catalog**

   - `order-service/src/controllers/OrderController.java` injecte toujours `repositories.PlantRepository` (de `utils`).
   - 👉 À faire : remplacer cet accès par un client HTTP (ex. `java.net.http.HttpClient`) utilisant un endpoint interne du `catalog-service`. L’interface `ApiMapper.PlantLookup` existe déjà, on peut fournir une implémentation “HTTP” locale à order.

2. **Routage JDBC par service (optionnel mais recommandé)**

   - Si on garde une seule BDD physique, rien d’autre à faire. Si l’on veut pouvoir pointer vers des bases distinctes à l’avenir, prévoir dans `ServiceLauncher` un `-Dservice.name` et adapter `DatabaseFactory` pour choisir `SERVICE_NAME_DATABASE_URL` quand elle existe.

## 4. Répertoires / fichiers utiles

- `Makefile` : cibles DB/seed/maven-workspace (lignes ~1-190).
- `config/.env` : ports + creds.
- `auth-service`, `catalog-service`, `order-service`, `user-service` : chacun contient `src/` + `db/` (schem+seed) + `resources/` + fast-jar via `utils.QuarkusBundleBuilder`.
- `utils/` :
  - `src/models` (DTO partagés : `User`, `Plant`, `Order`, `OrderItem`).
  - `src/repositories` (base repos, `UserRepository`, `PlantRepository`, etc.).
  - `src/util` (ApiMapper, PasswordUtil, ForwardedIdentity, Pm2Manager, QuarkusBundleBuilder, ServiceLauncher…).
- `Etude.txt` : analyse Monolithe vs Microservices & dépendances actuelles.

## 5. Conseils pour la suite

- Prioriser la refonte `OrderController` → Catalog HTTP client pour supprimer la dernière dépendance directe.
- Éviter toute duplication de fichiers entre services : si une classe doit être partagée, la mettre dans `utils/`.
- Garder en tête l’ordre `make db-migrate` ➜ `make seed` ➜ `make compile` lors des tests.
- Utilise ce script externe `~/scripts/Project_summaries/Java/generate_java_jpa_project_summary.sh <chemin_du_projet>` (ex. `~/scripts/Project_summaries/Java/generate_java_jpa_project_summary.sh /chemin/vers/plant_shop_java_quarkus_microservices`). Ce script concatène tous les fichiers du projet et ajoute l’arborescence (`tree`) dans un `projet.txt` à la racine cible : pense à lire ce fichier après l’exécution pour avoir une vue d’ensemble du code.


Important :

  repositories.PlantRepository par un client HTTP encapsulant le contrat ApiMapper.PlantLookup, afin
       d’éliminer la dépendance Catalog.
    2. Ajouter l’implémentation locale de ce client (par ex. java.net.http.HttpClient) et sa configuration côté order-service pour pointer vers l’API interne du catalog-service sans dupliquer de
  logique.
  => la relfexion dans Etude.txt précise bie  pour le 2 t-> copie les fichier nécessair,e puis gardez unkquement dans son contenu ce qui est iundpsienable au servi ce, donné, -> on reste DRY (trsè
  impoertant)
