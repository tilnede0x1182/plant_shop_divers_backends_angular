# 🌿 PlantShop – E-commerce Botanique (Angular / NestJS / Nx / Prisma)

Application complète de vente de plantes construite avec **Angular** (frontend) et **NestJS** (backend), orchestrée par **Nx** dans un monorepo modulaire.
Elle propose une interface moderne côté client 🌱 et un espace d’administration sécurisé 🔐 pour gérer plantes, utilisateurs et commandes.
La base PostgreSQL est alimentée par un **seed réaliste** (plantes 🪴, comptes 👤, commandes 📦).

---

## 🛠 Stack Technique

### 🎨 Frontend

* Angular 20 pour le rendu applicatif
* Angular Universal (SSR) pour le rendu côté serveur en production
* Nx 21 pour la gestion du monorepo (serve, build, tests, orchestration front/back)
* Bootstrap 5 pour la mise en forme rapide et responsive
* Proxy Angular → NestJS via `proxy.conf.json` en mode SPA

### 🧩 Backend

* NestJS 11 pour l’API REST et l’intégration SSR Angular Universal en mode production
* Prisma ORM (PostgreSQL, migrations, seed réaliste)
* Nx 21 également côté backend pour le build, le serve et la modularité des apps
* Authentification sécurisée via JWT + cookies httpOnly
* Guards NestJS (AuthGuard, AdminGuard) pour protéger les routes sensibles
* Middleware Angular/Nest pour restreindre l’accès à `/admin`

### ⚙️ Outils

* FakerJS pour les données factices
* BcryptJS pour le hachage des mots de passe
* Nx CLI pour builds/tests
* Tests end-to-end (NestJS + script Node)

---

## ✨ Fonctionnalités

### 👥 Côté client

* Catalogue des plantes (filtrage par stock > 0, tri alphabétique)
* Détail produit (nom, description, prix, stock)
* Panier (quantités ajustables, total dynamique, persistance locale)
* Commandes (création + historique)
* Compte utilisateur (inscription / connexion, profil modifiable)

### 🔧 Administration

* CRUD complet des plantes
* Gestion des utilisateurs avec rôles (`USER`, `ADMIN`)
* Consultation et gestion des commandes
* Sécurité via Guards côté serveur & client

---

## 🚀 Installation et lancement

### 🔧 Prérequis

* Node.js ≥ 18
* PostgreSQL ≥ 13
* npm ou pnpm installé

### ⚙️ Étapes principales

```bash
# 1) Installer les dépendances
pnpm install

# 2) Créer la base et exécuter les migrations Prisma
npx prisma migrate dev

# 3) Alimenter la base avec données factices
make seed
```

### 🖥️ Modes de lancement

#### Développement

```bash
# Backend seul (NestJS API)
make run-dev-back   # http://localhost:4100/api

# Frontend seul (Angular SPA avec proxy backend)
make run-dev-front  # http://localhost:8300

# SSR Angular Universal (Angular + Nest)
make run            # http://localhost:4150
```

#### Production

```bash
# Build front + back
make build

# Lancer le serveur SSR en prod
make prod           # http://localhost:4150
```

---

## 🧪 Test des routes Nest (back-end)

```bash
# Lancer serveur Nest en mode test
make test-e2e

# Lancer script de tests complets sur les routes
make test-routes
```

---

## 📦 Structure du projet

```
apps/
 ├─ plant-shop-angular-universal   → Frontend Angular Universal
 └─ plant_shop_nest                → Backend NestJS

prisma/    → Modèles + seed
tests/     → Test des routes backend
```
