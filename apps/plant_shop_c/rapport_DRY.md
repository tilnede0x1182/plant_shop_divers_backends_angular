# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Ce rapport couvre le backend C `plant_shop_c`, implémenté autour de Mongoose et de PostgreSQL. L'objectif est d'éliminer les répétitions identifiées en se basant sur le format établi par le projet Rust.

---

## Violations DRY dans **plant_shop_c**

### 1. Réponse JSON clonée dans chaque contrôleur - 🔴 Critique
Chaque contrôleur (`plant_controller.c:13-18`, `order_controller.c:41-47`, `order_item_controller.c:14-20`, `user_controller.c:12-18`) redéclare une fonction `send_json_reply` identique pour sérialiser un `cJSON` et écrire la réponse HTTP. Toute évolution (en-têtes, logs, compression) doit donc être répétée quatre fois. **Action** : créer un utilitaire public (ex. `src/utils/http.h/.c`) exposant `send_json_reply` et l’inclure partout.

### 2. Vérification administrateur recopiée - 🟠 Haute
Les fonctions `static int is_admin(struct mg_http_message* hm)` sont dupliquées dans plusieurs contrôleurs (`plant_controller.c:20-23`, `order_controller.c:14-48`, `order_item_controller.c:21-24`, `user_controller.c:19-24`). Elles appellent toutes `get_current_user_id` puis `user_repo_is_admin`. **Action** : exposer un helper `require_admin(struct mg_http_message* hm)` qui encapsule cette logique et retourne directement `AppError`. On évite ainsi les multiples variations et les oublis de contrôle d’erreurs.

### 3. Parsing JSON + messages d’erreur répétés - 🟠 Haute
Les handlers d’écriture (par ex. `plant_controller.c:66-107`, `order_controller.c:52-82`, `user_controller.c:24-90`) répètent exactement le même schéma : `cJSON_ParseWithLength`, vérification, réponse `400`, puis extraction de champs. Cette duplication rend fragile la validation (certains oublient de libérer le JSON ou n’uniformisent pas les messages). **Action** : créer une fonction `parse_json_or_400(struct mg_connection*, struct mg_http_message*, cJSON** out)` qui gère parsing, erreurs et log, puis la réutiliser dans tous les endpoints de type POST/PATCH.

---

## Impact estimé des refactorings

| Refactoring proposé                             | Lignes éliminées | Fichiers touchés | Complexité |
|------------------------------------------------|------------------|------------------|------------|
| Mutualiser `send_json_reply`                   | ~50              | 4 contrôleurs    | Basse      |
| Centraliser `is_admin` / garde administrateur  | ~35              | 4 contrôleurs    | Basse      |
| Factoriser le parsing JSON + réponses d’erreur | ~70              | 3 contrôleurs    | Moyenne    |

Total attendu : ~150 lignes en moins et un flux d’erreurs unifié.

---

## Conclusion
Le backend C implémente les mêmes opérations CRUD dans plusieurs fichiers sans mutualiser les briques communes (réponse JSON, garde admin, parsing). En appliquant les trois refactorings proposés, on obtient un chemin unique pour les réponses HTTP et une validation homogène. Priorité immédiate : extraire `send_json_reply` et la garde administrateur avant de traiter d’autres duplications plus fines (DTO, logs).***
