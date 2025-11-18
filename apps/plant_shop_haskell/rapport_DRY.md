# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Ce rapport vise le backend Haskell (Scotty + PostgreSQL) : `Controllers.PlantController`, `Controllers.OrderController`, `Controllers.UserController`, etc.

---

## Violations DRY

### 1. Requêtes SQL Plant copiées dans plusieurs modules - 🔴 Critique
`Controllers.PlantController` définit `plantSelectBase` et `selectPlantRows` (`src/Controllers/PlantController.hs:21-66`), tandis que `Controllers.OrderController` redéclare `plantSelectSql` (`src/Controllers/OrderController.hs:166-168`) et interroge directement la même table pour enrichir les items. Cette répétition fait qu’un changement de schéma (colonne renommée, cast `price::int`) doit être répliqué à deux endroits. **Action** : créer un module `Repositories.Plant` (ou réutiliser `Models.Plant`) exposant `fetchPlant`, `fetchAllPlants`, `selectPlantBase` et le réutiliser partout.

### 2. Accès utilisateur répété avec `SELECT * FROM users` - 🟠 Haute
Dans `UserController`, l’instruction `SELECT * FROM users WHERE id = ?` apparaît 4 fois (`src/Controllers/UserController.hs:37-48`, `50-65`, `86-94`, `96-100`). Les vérifications admin/propriétaire et la conversion `toPublicUser` y sont recopiées. **Action** : déplacer la lecture/écriture des utilisateurs (find/list/update/delete) dans un module dédié (`Repositories.User`) et exposer des helpers `getUserForResponse`, `ensureUserExists`.

### 3. Sélection d’ordres basée sur des chaînes SQL concaténées - 🟠 Haute
`OrderController` maintient quatre constantes `orderSelectBase/orderSelectAdmin/orderSelectByUser/orderSelectById` (`src/Controllers/OrderController.hs:22-40`) qui ne diffèrent que par un `WHERE` ou `ORDER BY`. Cette duplication déclenche déjà des incohérences de tri. **Action** : implémenter un builder (ou des fonctions `selectOrders :: OrderFilter -> Query`) évitant de concaténer des strings dans chaque handler, puis partager la logique dans `fetchFullOrder`.

---

## Impact estimé

| Refactoring proposé                                | Lignes supprimées | Modules touchés            | Complexité |
|----------------------------------------------------|-------------------|----------------------------|------------|
| Module `Repositories.Plant` partagé                | ~80               | PlantController, OrderCtrl | Moyenne    |
| Module `Repositories.User` + helpers publics       | ~70               | UserController             | Moyenne    |
| Builder de requêtes `orders`                       | ~60               | OrderController            | Basse      |

---

## Conclusion
L’usage massif de requêtes SQL en ligne viole directement DRY : la même clause `SELECT ... FROM plants` et les mêmes conversions `price::int` existent dans plusieurs modules. Centraliser l’accès aux données (repositories) est la première étape pour sécuriser ce backend fonctionnel tout en respectant la consigne DRY du monorepo.***
