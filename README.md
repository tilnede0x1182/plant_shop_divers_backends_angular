# 🔬 Multi-Backends Playground – Exploration backend avec Angular

Ce repo Nx est un **laboratoire d’exploration backend** : un frontend Angular SSR unique sert d’interface, et plusieurs backends interchangeables exposent la même API REST et partagent la même base PostgreSQL.
Le cas d’usage est simple (PlantShop), mais il permet de comparer des backends sur un terrain concret : modèles, authentification, commandes.

---

## 🎯 Objectif

* Avoir un **frontend unique et stable** (Angular 20 + Universal) qui reste inchangé.
* Développer et brancher **plusieurs backends différents** sur ce même frontend.
* Vérifier que tous respectent un contrat API identique (mêmes routes, mêmes modèles, même schéma PostgreSQL).
* Étudier la façon dont chaque backend gère : l’authentification, la modularité, la connexion à la base et l’organisation du code.

---

## 🛠 Stack utilisée

### 🎨 Frontend (socle commun)

* Angular 20 + Angular Universal (SSR)
* Nx 21 pour l’orchestration monorepo
* Bootstrap 5 pour la mise en forme
* Proxy Angular → backend actif

### 🗄️Base de données

* PostgreSQL 15.x
* Prisma 5.x pour les migrations et seeds

---

## 📚 Contrat API (routes communes à tous les backends)

### Authentification

* `POST /auth/register` → inscription
* `POST /auth/login` → connexion
* `POST /auth/logout` → déconnexion
* `GET /auth/me` → infos utilisateur connecté

### Plantes

* `GET /plants` → liste publique
* `GET /plants/:id` → détail
* `POST /admin/plants` → création (admin)
* `PATCH /admin/plants/:id` → mise à jour (admin)
* `DELETE /admin/plants/:id` → suppression (admin)

### Utilisateurs

* `PATCH /users/:id` → édition de son profil (ou par admin)
* `GET /admin/users` → liste (admin)
* `PATCH /admin/users/:id` → mise à jour (admin)
* `DELETE /admin/users/:id` → suppression (admin)

### Commandes

* `GET /orders` → liste des commandes utilisateur
* `POST /orders` → création d’une commande avec items
* `PATCH /orders/:id` → mise à jour (admin)
* `DELETE /orders/:id` → suppression (admin)

## 🧩 Backends

* **NestJS 11** : backend de référence, API REST + SSR Angular Universal.
* **Manifest 4.17.8** : backend déclaratif piloté par `manifest.yml` (entités, policies, endpoints) où la CLI Manifest orchestre les handlers Node custom + seeds PostgreSQL pour exposer l’API PlantShop.
* **Go (GORM)** : backend en Go structuré, sans framework web tiers, API REST implémentée manuellement avec `net/http`, GORM et JWT cookie sécurisé.
* **Rust (Poem + SQLx)** : backend Rust moderne, basé sur le framework `poem` pour les routes HTTP et `sqlx` pour la base de données.
* **Rust (Poem + SeaORM)** : backend Rust moderne, basé sur le framework `poem` pour les routes HTTP et `SeaORM` pour la gestion de la base de données.`
* **C (Natif)** : serveur HTTP minimaliste en C11, basé sur `mongoose`, MVC manuel, gestion PostgreSQL via `libpq`.
* **C++ (Drogon)** : serveur HTTP moderne en C++17, basé sur le framework `Drogon` avec ORM intégré, compilation via `CMake`.
* **Java (Natif)** : serveur HTTP minimaliste en Java 21, utilisant `HttpServer` natif, MVC manuel.
* **Java (Javalin)** : backend Java léger, utilisant le framework `Javalin` pour les routes HTTP.
* **Java (Quarkus / RESTEasy)** : backend Java structuré autour de Quarkus, utilisant RESTEasy et Undertow pour l’API HTTP, avec injection de dépendances CDI et mapping JAX-RS.
* **Java (Micronaut)** : backend Java performant utilisant le framework `Micronaut` pour gérer les routes HTTP, l’injection de dépendances et la configuration applicative.
* **Java (Java Lite avec active web)** : backend Java MVC utilisant le framework JavaLite ActiveWeb avec Jetty pour le serveur HTTP et ActiveJDBC pour la base de données.
* **Java (Spring Boot Security avec JDBC manuel)** : backend Java MVC utilisant le framework Spring Boot Security avec Tomcat embarqué pour le serveur HTTP et une gestion JDBC directe pour la base de données.
* **Java (microservices HTTP distribués avec sessions partagées)** : backend Java modulaire en architecture distribuée utilisant le serveur HTTP natif pour une gateway et quatre services (auth, catalogue, commandes, utilisateurs), avec authentification par cookie de session, routage centralisé et accès PostgreSQL en JDBC direct à partir d’un fichier d’environnement commun.
* **Java (Spring Boot Security avec Hibernate)** : backend Java MVC utilisant Spring Boot 3 et Spring Security 6 avec Tomcat embarqué pour le serveur HTTP et Spring Data JPA/Hibernate pour la base de données PostgreSQL.
* **Python (Flask)** : backend minimaliste REST avec `Flask`, connexion PostgreSQL via `psycopg2`.
* **Haskell (Stack)** : serveur HTTP en style fonctionnel avec `Scotty`, architecture MVC claire, ORM léger via `postgresql-simple`, sécurité JWT

---

## 📦 Structure du repo

```
## 📦 Structure du repo

apps/
  ├─ plant-shop-angular-universal                    → Frontend Angular + SSR
  ├─ plant_shop_nest                                 → Backend NestJS
  ├─ plant_shop_manifest                             → Backend Manifest
  ├─ plant_shop_go                                   → Backend Golang avec GORM
  ├─ plant_shop_cpp                                  → Backend en C++ (Drogon)
  ├─ plant_shop_c                                    → Backend en C (HTTPServer)
  ├─ plant_shop_c-sharp                              → Backends .NET (Dapper, EF Core, MVC C#)
  ├─ plant_shop_node_js                              → Backends Node.js/TypeScript MVC
  ├─ plant_shop_java
  │  ├── Structure_en_micro_services                 →
  │  │  ├── plant_shop_java_microservices            → Microservices Java (HTTP)
  │  │  ├── plant_shop_java_javalin_microservices    → Microservices Java avec Javalin
  │  │  ├── plant_shop_java_micronaut_microservices  → Microservices Java avec Micronaut
  │  │  ├── plant_shop_java_quarkus_microservices    → Microservices Java avec Quarkus
  │  │  └── plant_shop_java_spring_microservices     → Microservices Java avec Spring Boot
  │  ├── Structure_en_application_distribuée         →
  │  │  ├── plant_shop_java_distribuée               → Architecture distribuée (HTTP)
  │  │  ├── plant_shop_java_javalin_distribuée       → Architecture distribuée avec Javalin
  │  │  ├── plant_shop_java_micronaut_distribuée     → Architecture distribuée avec Micronaut
  │  │  ├── plant_shop_java_quarkus_distribuée       → Architecture distribuée avec Quarkus
  │  │  └── plant_shop_java_spring_distribuée        → Architecture distribuée avec Spring Boot
  │  └── Structure_monolithique                      →
  │     ├── plant_shop_java                          → Backend en Java avec HTTP
  │     ├── plant_shop_java_active_web               → Backend Java ActiveWeb
  │     ├── plant_shop_javalin                       → Backend Java Javalin
  │     ├── plant_shop_java_micronaut                → Backend Java Micronaut
  │     ├── plant_shop_java_quarkus                  → Backend Java Quarkus
  │     └── plant_shop_java_spring                   → Backend Java Spring Boot
  ├─ plant_shop_python                               → Backend en Python (Flask)
  ├─ plant_shop_haskell                              → Backend en Haskell (Stack)
  └─ plant_shop_rust
     ├─ plant_shop_rust_sqlx                         → Backend Rust avec SQLx
     └─ plant_shop_rust_see_orm                      → Backend Rust avec SeaORM

prisma/                                                        → Modèles + seed
tests/                                                         → Scripts de test des routes backend
diagnose-ora.js                                                → Script diagnostic
```
