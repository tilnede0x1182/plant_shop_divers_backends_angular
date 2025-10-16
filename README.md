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
* **Manifest 4.17.8** : backend déclaratif en YAML, API REST branchée sur PostgreSQL. ABANDON (non débuggué)
* **Go (GORM)** : backend en Go structuré, sans framework web tiers, API REST implémentée manuellement avec `net/http`, GORM et JWT cookie sécurisé.

---

## 📦 Structure du repo

```
apps/
 ├─ plant-shop-angular-universal   → Frontend Angular
 ├─ plant_shop_nest                → Backend NestJS
 ├─ plant_shop_manifest            → Backend Manifest
 └─ plant_shop_go                  → Backend Golang
prisma/    → Modèles + seed
tests/     → Scripts de test des routes backend
```
