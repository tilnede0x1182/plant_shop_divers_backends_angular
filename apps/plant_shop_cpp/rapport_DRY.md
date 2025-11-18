# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Analyse du backend C++ `plant_shop_cpp` (Drogon + ORM généré) couvrant les contrôleurs Auth/Plants/Orders/OrderItems/Users.

---

## Violations DRY dans **plant_shop_cpp**

### 1. Helper `err()` recopié dans chaque contrôleur - 🔴 Critique
`PlantController.cpp:17-24`, `OrderController.cpp:27-33`, `OrderItemController.cpp:15-21`, `UserController.cpp:17-24` et `AuthController.cpp:98-105` déclarent tous la même fonction `static HttpResponsePtr err(...)`. Cette redondance prolifère (5 copies) et empêche toute personnalisation globale (headers, i18n). **Action** : déplacer `err()` dans un utilitaire (ex. `controllers/HttpError.h`) ou dans `BaseController`, puis l’exposer via un namespace partagé.

### 2. Résolution JWT/JSESSIONID ré-implémentée - 🟠 Haute
`AuthController::isAdmin` (`controllers/AuthController.cpp:107-133`) et `AuthController::canActDecodeJWT` (`controllers/AuthController.cpp:144-175`) contiennent deux fois la même séquence : lecture du cookie `jwt`, parsing via `parseToken`, fallback sur `JSESSIONID` + `sessionStore`. L’absence de mutualisation rend les corrections (nom du cookie, claims) très risquées. **Action** : extraire un helper `resolveSession(const HttpRequestPtr&)` retournant soit les claims, soit un `std::optional<User>` et l’utiliser pour `isAdmin`, `canAct`, `canActDecodeJWTBool`.

### 3. Conversion prix/DTO dispersée - 🟠 Haute
Plusieurs méthodes convertissent manuellement les prix SQL (stockés en texte) vers `int/double` : `PlantController::getPlant` (`controllers/PlantController.cpp:40-50`), `OrderController::createOrder` (`controllers/OrderController.cpp:47-90`) et `OrderController::listOrders` (`controllers/OrderController.cpp:150-170`). Le même code `std::stod`/`math` est recopié, ce qui a déjà généré des incohérences (arrondis différents). **Action** : introduire des mappers (`PlantDto fromModel(const Plants&)`, `OrderDto buildOrder(...)`) qui encapsulent conversion et tri.

---

## Impact estimé des refactorings

| Refactoring proposé                                | Lignes éliminées | Fichiers touchés | Complexité |
|----------------------------------------------------|------------------|------------------|------------|
| Mutualiser `err()`                                 | ~60              | 5 contrôleurs    | Basse      |
| Extraire `resolveSession` (JWT + JSESSIONID)       | ~80              | AuthController   | Moyenne    |
| Centraliser les conversions prix / DTO             | ~70              | Plants + Orders  | Moyenne    |

---

## Conclusion
Le projet C++ souffre principalement de duplications structurelles (gestion des erreurs, résolution des sessions, mapping JSON). Centraliser ces responsabilités permettra de fiabiliser les changements futurs (changement de cookie, ajout de headers) tout en respectant le principe DRY imposé à l’ensemble du monorepo.***
