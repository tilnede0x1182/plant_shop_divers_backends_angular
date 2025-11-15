# Audit qualité – microservices Java HTTP

**Date** : 13 novembre 2025

## Périmètre analysé

- Microservices "classiques" basés sur `com.sun.net.httpserver` (auth, catalog, order, user, gateway) dans `apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_microservices`.
- Microservices basés sur Javalin (auth, catalog, order, user, gateway) dans `apps/plant_shop_java/Structure_en_micro_services/plant_shop_java_javalin_microservices`.
- Base utilitaire partagée (`utils`, `config`, `gateway`).

## Forces communes

- Modélisation métier claire : records pour Order/Plant/User côté HTTP et mappers (`ApiMapper`) côté Javalin.
- Hashage centralisé via BCrypt (`util/PasswordUtil`) qui limite les erreurs de sécurité autour des mots de passe.
- Gateway responsable de la propagation d’identité (`X-User-Id`/`X-User-Admin`) et de la protection des routes publiques vs privées.

## Risques critiques

1. **Authentification cassée côté order-service HTTP** : `AuthContext.userId()` déclenche une `IllegalStateException` pour tout utilisateur anonyme, ce qui façonne une réponse 500 au lieu d’un 401 et bloque l’utilisation de l’API (`apps/.../order-service/src/controller/OrderController.java:117-135`).
2. **Sessions en mémoire locale partout** : que ce soit `AuthController.sessions` (HTTP/Javalin) ou `SessionService.sessions` (Micronaut/Quarkus), il n’y a ni TTL, ni réplication, ni revocation contraignante. Toute instance redémarrée perd ses sessions et il devient impossible de scaler ou de forcer la déconnexion d’un cookie volé.
3. **Couplage direct à la même base PostgreSQL** : les services accèdent librement aux tables des autres domaines (order ⇄ catalog), ce qui casse l’isolation et rend toutes les migrations dangereuses. Qu’un service écrase une table impacterait tous les autres.
4. **Gateway fragile** : `GatewayConfig.port()` analyse `SERVER_ADDRESS` comme un entier (`apps/.../gateway/src/Main.java:51-63`). Si la variable contient un schéma (`http://localhost:4100`), la gateway lève `NumberFormatException` et toute la chaîne ne démarre plus.

## Problèmes majeurs (court/moyen terme)

1. **Duplication des BaseController HTTP** : chaque service redéclare sa propre logique de parsing JSON, d’en-têtes, d’erreurs et de gestion d’exceptions. Cela empêche l’introduction de middlewares transverses (rate limit, tracing, métriques) et rend les correctifs fastidieux.
2. **Transactions fragiles côté Javalin** : `OrderController.create()` manipule manuel `db.setAutoCommit(false)` sur une connexion partagée (voir `apps/.../order-service/src/controllers/...`), ce qui expose d’autres threads à des connexions en attente et ne garantit pas la compensation de l’appel HTTP vers `/internal/plants/{id}/stock`.
3. **Javalin embarque tous les contrôleurs** : `ApplicationController` (catalog/order/user/auth dans chaque dossier) fuse toutes les routes d’un monolithe dans un seul service, ce qui annule l’isolation/microservice promise et multiplie les dépendances SQL.
4. **Validation de données insuffisante** : on peut créer/mettre à jour une plante avec un `price` nul/négatif et un `stock` non borné ; les utilisateurs ne sont soumis à aucun contrôle de force de mot de passe ni de format d’email, donc les formulaires acceptent des données invalides.
5. **Observabilité faible** : la production affiche encore des `System.out.printf` et `e.printStackTrace` (OrderController, AuthService, Gateway) sans journalisation structurée ni corrélation de requêtes, ce qui rend quasi impossible le débogage en production.

## Opportunités d’amélioration rapides

1. Extraire un module `http-core` (framework léger) pour mutualiser parsing JSON, gestion des exceptions et auth, et remplacer `com.sun.net.httpserver` par un cadre testable (Javalin/Helidon).
2. Remplacer les `ConcurrentHashMap` de session par un store partagé (Redis, Postgres, JWT) avec TTL, rotation des cookies et révocation centralisée.
3. Isoler les bases : chaque microservice devrait posséder son propre schéma ou au minimum des vues/permissions strictes pour empêcher les écritures croisées.
4. Corriger la configuration de la gateway (séparer `SERVER_HOST` et `SERVER_PORT`, valider au démarrage et afficher un message clair).
5. Ajouter des tests d’intégration (Postman/Newman, RestAssured, etc.) couvrant les scénarios critiques : commande avec stock insuffisant, user non-admin sur `/admin`, expiration de session.
6. Pour Javalin, scinder réellement les services (auth, catalog, order, user) au lieu d’exposer tous les contrôleurs depuis chaque projet.

## Priorisation recommandée

- **Immédiat** : réparer `AuthContext`/`OrderController`, valider les headers (401 au lieu de 500), sécuriser les sessions (TTL/rotation) et séparer les routes dans la gateway.
- **1-2 itérations** : mettre en place des migrations versionnées, des transactions sûres (outbox/saga si nécessaire) et des tests automatisés.
- **Moyen terme** : investir dans l’observabilité (logs structurés, traces, métriques) et clarifier les frontières entre les variantes HTTP brut, Javalin et autres pour éviter la dérive monolithique.

---

## Autres microservices Java (Micronaut / Quarkus / Spring Boot Security)

Ces variantes utilisent des frameworks plus expressifs mais partagent toujours la même base PostgreSQL et un stockage de session en mémoire.

### Micronaut (`apps/.../plant_shop_java_micronaut_microservices`)

- **Forces** : injection automatique du `Connection`, filtres `Guards`/`SessionAuthFilter`, annotations `@Controller/@Get` et `ApiMapper` pour homogénéiser les réponses.
- **Risques** : sessions stockées dans `ConcurrentHashMap` sans TTL ni persistence, `OrderController.create()` manipule le `autoCommit` d’une `Connection` partagée et n’applique pas de compensation quand l’appel PATCH vers `/internal/plants/{id}/stock` échoue.
- **Bugs spécifiques** : `PlantController.update()` ignore les mises à jour de `stock = 0`, empêchant de vider un article, et accepte des `price` négatifs ; l’appel interne `/internal/plants/{id}/stock` ne protège pas le stock d’un double négatif.
- **Gateway** : même défaut que les services HTTP (parsing de `SERVER_ADDRESS` comme entier).

### Quarkus (`apps/.../plant_shop_java_quarkus_microservices`)

- **Forces** : CDI + `@Transactional` garantissent des DAO cohérents et `SessionAuthFilter` injecte l’utilisateur dans `AuthenticatedUser` pour les guards.
- **Risques** : sessions toujours en mémoire (`SessionService.sessions`) sans synchronisation ; la mise à jour du stock externe reste hors transaction parce qu’il s’agit d’un appel HTTP, donc toute erreur après la création des items laisse l’inventaire et la commande en désynchronisation.
- **Observabilité** : les logs dans les filtres (catch Exception puis `System.err`) masquent les erreurs DB et entraînent des 401 silencieux.

### Spring Boot Security (`apps/.../plant_shop_java_spring_boot_security_microservices`)

- **Forces** : structure Spring standard, `Guards`/`AuthenticatedUser` pour encapsuler l’identité, `ApiMapper` partagé.
- **Risques** : `securityFilterChain` autorise toutes les requêtes (`auth.anyRequest().permitAll()`) et s’appuie sur les `Guards` manuels, donc un oubli expose une route. De plus, `OrderController.create()` n’est pas `@Transactional`, ce qui peut laisser une commande partiellement créée si la mise à jour de `/internal/plants/{id}/stock` échoue.
- **Comportements suspects** : impossible de mettre à jour le `stock` à zéro, les `price` négatifs ne sont pas bloqués, l’authentification repose sur un cookie `session_id` non protégé contre le vol (pas de SameSite/secure configurable).

## Observations transversales

- Les seeds, repos et DTO sont dupliqués dans chaque variante, ce qui complique les migrations lorsque les tables changent (déplacer la logique SQL vers un module commun serait plus propre).
- Les appels HTTP vers `/internal/plants/{id}/stock` ne sont jamais atomiques : aucun pattern saga/compensation n'est implémenté, donc les systèmes se désynchronisent dès qu'une mise à jour échoue à mi-parcours.
- Le stockage de session en RAM reste la principale faiblesse : pas de TTL/rotation/révocation centralisée, donc impossible de gérer les déconnexions forcées ou la réplication multi-nœuds.

## Analyse des actions concrètes à faire

### plant_shop_java_javalin_microservices

**Fichiers à corriger (copiés sans adaptation) :**

1. `*/src/controllers/ApplicationController.java` dans chaque service

   - **Problème** : Contient TOUTES les routes (auth, catalog, order, user) au lieu des routes spécifiques au service
   - **Action** : Ne garder que les routes du domaine (auth-service → routes /auth uniquement)

2. `*/src/util/ApiMapper.java` dans chaque service

   - **Problème** : Contient toUser(), toPlant(), toOrder(), toOrderItem() dans tous les services
   - **Action** : auth/user-service → toUser() uniquement, catalog-service → toPlant(), order-service → toOrder()/toOrderItem()

3. `order-service/src/models/User.java` et `Plant.java`
   - **Problème** : Modèles du domaine externe copiés inutilement
   - **Action** : Supprimer ou remplacer par DTOs minimalistes si besoin

### plant_shop_java_micronaut_microservices

**Fichiers à corriger (copiés sans adaptation) :**

1. `*/src/util/ApiMapper.java` dans chaque service

   - **Problème** : Méthodes pour tous les modèles dans chaque service
   - **Action** : auth/user-service → toUser(), catalog-service → toPlant(), order-service → toOrder()/toOrderItem()

2. `order-service/src/models/User.java` et `Plant.java`
   - **Problème** : Modèles complets copiés au lieu de DTOs
   - **Action** : Créer des DTOs légers ou interfaces si nécessaire

### plant_shop_java_quarkus_microservices

**Fichiers à corriger (copiés sans adaptation) :**

1. `*/src/util/ApiMapper.java` dans chaque service

   - **Problème** : Contient toutes les méthodes de mapping
   - **Action** : Garder uniquement les méthodes du domaine du service

2. `order-service/src/models/` fichiers externes au domaine
   - **Problème** : User.java, Plant.java copiés intégralement
   - **Action** : Utiliser des DTOs ou supprimer si pas nécessaire

### plant_shop_java_spring_microservices

**Fichiers à corriger (copiés sans adaptation) :**

1. `*/src/util/ApiMapper.java` dans chaque service

   - **Problème** : Duplication de toutes les méthodes de transformation
   - **Action** : Ne garder que les transformations du domaine

2. `*/src/security/Guards.java` et `AuthContext.java`

   - **Problème** : Copiés partout même dans catalog-service qui n'a pas besoin d'auth complexe
   - **Action** : Simplifier ou supprimer dans les services qui font uniquement de la validation de token

3. `order-service/src/models/` modèles externes
   - **Problème** : User.java et Plant.java complets au lieu de DTOs
   - **Action** : Créer des représentations minimales pour les besoins inter-services

## Actions concrètes à faire

### plant_shop_java_javalin_microservices

**PROBLÈME CRITIQUE IDENTIFIÉ** : Tous les ApplicationController appellent `AuthController.getSessions()` mais AuthController n'existe QUE dans auth-service. Le Makefile compile chaque service séparément, donc les autres services ne peuvent pas accéder à AuthController.

1. **auth-service/src/controllers/ApplicationController.java**

   - Supprimer PlantController, OrderController, UserController du constructeur (lignes 20-22, 27-29)
   - Supprimer toutes les routes sauf /api/auth/\* dans getRoutes() (garder lignes 40-45, supprimer 47-84)
   - GARDER authenticate() et requireUser/requireAdmin car utilisés par les routes auth

2. **catalog-service/src/controllers/ApplicationController.java**

   - IMPOSSIBLE de supprimer AuthController car ligne 112 appelle `AuthController.getSessions()` - PROBLÈME À CORRIGER
   - Supprimer OrderController, UserController du constructeur (lignes 21-22, 28-29)
   - Supprimer routes auth (40-45), users (62-68, 71-76), orders (78-84)
   - GARDER routes plants (47-59) et internal/plants (39-41)
   - GARDER User et UserRepository car authenticate() ligne 119 fait `userRepoForAuth.find(userId)`

3. **order-service/src/controllers/ApplicationController.java**

   - IMPOSSIBLE de supprimer AuthController car ligne 112 appelle `AuthController.getSessions()` - PROBLÈME À CORRIGER
   - Supprimer PlantController, UserController du constructeur (lignes 20-21, 27-28)
   - Supprimer toutes routes sauf /api/orders/\* (garder lignes 79-84, supprimer reste)
   - GARDER User et UserRepository car OrderController utilise User (lignes 54, 71) et authenticate() a besoin de UserRepository

4. **user-service/src/controllers/ApplicationController.java**

   - IMPOSSIBLE de supprimer AuthController car ligne 112 appelle `AuthController.getSessions()` - PROBLÈME À CORRIGER
   - Supprimer PlantController, OrderController du constructeur (lignes 20, 22, 27, 29)
   - Supprimer routes auth (40-45), plants (47-59), orders (78-84)
   - GARDER routes users et admin/users (62-76)

5. **Modèles et dépendances**
   - User.java NÉCESSAIRE dans auth-service et user-service (chacun a son propre)
   - PlantStock.java dans order-service est correct (DTO minimaliste)
   - PROBLÈME : Les sessions sont en mémoire dans AuthController, inaccessibles aux autres services

### plant_shop_java_micronaut_microservices

**PROBLÈME ARCHITECTURE** : La Gateway PROPAGE bien l'identité (X-User-Id, X-User-Admin) mais les services tentent d'utiliser Guards qui n'existe pas et cherche dans request.getAttribute() au lieu des headers.

1. **auth-service** - FONCTIONNEL

   - SessionAuthFilter met l'utilisateur dans request.setAttribute("user")
   - Guards.requireUser() récupère cet attribut
   - GARDER tous les fichiers security/

2. **catalog-service** - NE COMPILE PAS

   - PlantController ligne 43, 58, 66 : `Guards.requireAdmin(request)` mais Guards n'existe pas
   - SOLUTION : Créer un Guards local qui lit X-User-Admin depuis les headers

3. **order-service** - NE COMPILE PAS

   - OrderController ligne 51, 64 : `Guards.requireUser(request)` mais Guards n'existe pas
   - SOLUTION : Créer Guards local + User DTO minimal (id, isAdmin)

4. **user-service** - NE COMPILE PAS

   - UserController lignes 40, 48, 76, 84, 96 : utilise Guards qui n'existe pas
   - SOLUTION : Créer Guards local qui lit les headers X-User-Id/X-User-Admin

5. **gateway/src/Routes.java** - CORRECT

   - Lignes 88-91 : Propage X-User-Id et X-User-Admin aux services
   - NE PAS MODIFIER

6. **utils/src/ApiMapper.java**
   - SUPPRIMER de utils car ne compile pas sans tous les modèles
   - CRÉER ApiMapper local dans chaque service avec UNIQUEMENT les méthodes nécessaires

### plant_shop_java_quarkus_microservices

**PROBLÈME ARCHITECTURE** : Identique à Micronaut. La Gateway propage X-User-Id/X-User-Admin mais les services cherchent Guards dans AuthenticatedUser au lieu des headers.

1. **auth-service** - FONCTIONNEL

   - SessionAuthFilter remplit AuthenticatedUser
   - Guards utilise @Inject AuthenticatedUser
   - GARDER tous les fichiers security/

2. **catalog-service** - NE COMPILE PAS

   - PlantController utilise Guards qui n'existe pas
   - SOLUTION : Créer Guards local qui lit X-User-Admin depuis les headers

3. **order-service** - NE COMPILE PAS

   - OrderController ligne 40 : `@Inject Guards guards;` - Guards n'existe pas
   - Lignes 54, 69 : `guards.requireUser()` retourne User mais User n'existe pas
   - SOLUTION : Créer Guards + User DTO minimal

4. **user-service** - NE COMPILE PAS

   - UserController utilise Guards qui n'existe pas
   - SOLUTION : Créer Guards local

5. **gateway/src/Routes.java** - CORRECT

   - Lignes 88-91 : Propage X-User-Id et X-User-Admin
   - NE PAS MODIFIER

6. **utils/src/ApiMapper.java**
   - SUPPRIMER et créer ApiMapper local par service

### plant_shop_java_spring_boot_security_microservices

**BON POINT** : AuthContext.fromHeaders() dans utils/src/ lit CORRECTEMENT X-User-Id et X-User-Admin. C'est le modèle à suivre pour les autres frameworks !

1. **utils/** - DUPLICATION MASSIVE

   - Fichiers dupliqués dans utils/ ET utils/src/ (ApiMapper, AuthContext, PasswordUtil, etc.)
   - SUPPRIMER tous les fichiers dans utils/ et garder UNIQUEMENT utils/src/

2. **order-service** - NE COMPILE PAS

   - OrderController ligne 37 : `@Autowired Guards guards;` - Guards n'existe pas
   - Lignes 51, 65 : `guards.requireUser()` retourne User
   - SOLUTION : Créer Guards qui utilise AuthContext.fromHeaders()

3. **catalog-service et user-service** - PROBABLEMENT MÊME PROBLÈME

   - Vérifier s'ils utilisent Guards inexistant

4. **utils/src/ApiMapper.java**

   - Vérifier s'il importe tous les modèles
   - Si oui, créer ApiMapper local par service

5. **utils/src/AuthContext.java** - PARFAIT !
   - Lignes 22-35 : fromHeaders() lit X-User-Id et X-User-Admin
   - C'EST LA SOLUTION pour les autres frameworks

### plant_shop_java_spring_boot_security_hibernate_microservices

**MÊME PROBLÈMES que Spring Boot sans Hibernate**

1. **utils/** - DUPLICATION

   - Fichiers dupliqués dans utils/ ET utils/src/
   - SUPPRIMER utils/_.java et garder utils/src/_.java

2. **order-service** - NE COMPILE PAS

   - OrderController ligne 37 : `Guards guards;` - Guards n'existe pas
   - SOLUTION : Créer Guards qui utilise AuthContext.fromHeaders()

3. **Tous les services**
   - Même architecture que Spring Boot sans Hibernate
   - Mêmes solutions à appliquer

---

## État d'avancement des corrections

### ✅ FAIT - Isolation des microservices (15 novembre 2025)

#### Principe appliqué

Chaque service ne contient **que le code de son domaine**, avec duplication justifiée par l'architecture distribuée :

- Guards local lit les headers X-User-Id/X-User-Admin propagés par la gateway
- ApiMapper dupliqué avec **uniquement** les méthodes nécessaires au service
- UserDTO minimal au lieu de model.User complet pour les services non-auth
- **Principe DRY maintenu** à l'intérieur de chaque service
- **Architecture distribuée respectée** : duplication acceptée entre services isolés

#### plant_shop_java_javalin_microservices ✅

- ✅ catalog-service : Guards.java local (lecture headers), ApiMapper.java (toPlant uniquement)
- ✅ order-service : UserDTO.java minimal, Guards.java local, OrderController adapté (User → UserDTO)
- ✅ user-service : UserDTO.java, Guards.java local, ApiMapper.java (toUser uniquement), UserController adapté
- ✅ auth-service : ApiMapper.java local (toUser uniquement)

#### plant_shop_java_micronaut_microservices ✅

- ✅ catalog-service : Guards.java local (@Inject HttpHeaders), ApiMapper.java (toPlant)
- ✅ order-service : UserDTO.java, Guards.java local (retourne UserDTO), OrderController adapté
- ✅ user-service : UserDTO.java, Guards.java local, ApiMapper.java (toUser), UserController adapté
- ✅ auth-service : ApiMapper.java local (toUser)

#### plant_shop_java_quarkus_microservices ✅

- ✅ catalog-service : Guards.java local (@Inject HttpHeaders), ApiMapper.java (toPlant)
- ✅ order-service : UserDTO.java, Guards.java local (retourne UserDTO), OrderController adapté
- ✅ user-service : UserDTO.java, Guards.java local, ApiMapper.java (toUser), UserController adapté
- ✅ auth-service : ApiMapper.java local (toUser)

#### plant_shop_java_spring_boot_security_microservices ✅

- ✅ catalog-service : Guards.java local (@Component, RequestContextHolder), ApiMapper.java (toPlant)
- ✅ order-service : UserDTO.java, Guards.java local, OrderController adapté (User → UserDTO)
- ✅ user-service : UserDTO.java, Guards.java local, ApiMapper.java (toUser), UserController adapté
- ✅ auth-service : ApiMapper.java local (toUser)

#### plant_shop_java_spring_boot_security_hibernate_microservices ✅

- ✅ catalog-service : Guards.java local, ApiMapper.java (toPlant)
- ✅ order-service : UserDTO.java, Guards.java local, OrderController adapté
- ✅ user-service : UserDTO.java, Guards.java local, ApiMapper.java (toUser), UserController adapté
- ✅ auth-service : ApiMapper.java local (toUser)

**Total : 5 projets × 4 services = 20 services corrigés**

### ❌ RESTE À FAIRE (comparé aux projets monolithiques)

**Méthodologie** : Seules les fonctionnalités **déjà implémentées dans les projets monolithiques** (`~/code/.../Structure_monolithique/`) sont listées ici comme manquantes dans les microservices.

#### Sécurité des cookies (FAIT dans monolithes, MANQUANT dans microservices)

Les projets monolithiques (ex: `plant_shop_javalin/src/controllers/AuthController.java:64-67`) implémentent :

```java
cookie.setHttpOnly(true);
cookie.setSameSite(SameSite.LAX);
cookie.setSecure(false); // configurable
cookie.setMaxAge(3600);
```

**À faire dans microservices** :

- [ ] **Javalin microservices** : Ajouter `cookie.setSameSite(SameSite.LAX)` dans auth-service (actuellement ligne 60-63 manque SameSite)
- [ ] **Micronaut microservices** : Ajouter `.sameSite(Cookie.SameSite.Lax)` dans auth-service
- [ ] **Quarkus microservices** : Ajouter `.sameSite(SameSite.LAX)` dans auth-service
- [ ] **Spring Boot microservices** : Configurer `server.servlet.session.cookie.same-site=lax` dans application.properties

#### Problèmes architecturaux non résolus dans les monolithes

Les points suivants **ne sont PAS implémentés dans les monolithes** et sont donc **hors périmètre** :

- ❌ **Sessions en mémoire** : Les monolithes utilisent aussi `ConcurrentHashMap` (pas de Redis/JWT)
- ❌ **Validation métier stricte** : Les monolithes n'ont pas de validation price/stock négatif
- ❌ **Force mot de passe** : Aucune contrainte de complexité dans les monolithes
- ❌ **Protection CSRF** : Non implémentée dans les monolithes
- ❌ **Transactions distribuées** : N/A pour monolithes (pas d'appels HTTP inter-services)
- ❌ **Base de données partagée** : Spécifique microservices (isolation non pertinente pour monolithes)
- ❌ **Gateway fragile** : N/A pour monolithes (pas de gateway)

---

## Actions techniques à effectuer

### 1. plant_shop_java_micronaut_microservices

**Fichier** : `auth-service/src/controllers/AuthController.java`

**Ligne 60-63** (méthode `login()`) - Ajouter `.sameSite()` :

```java
// AVANT
Cookie cookie = Cookie.of("session_id", sessionId)
    .path("/")
    .httpOnly(true)
    .maxAge(3600);

// APRÈS
Cookie cookie = Cookie.of("session_id", sessionId)
    .path("/")
    .httpOnly(true)
    .maxAge(3600)
    .sameSite(Cookie.SameSite.Lax);
```

**Ligne ~75** (méthode `logout()`) - Ajouter `.sameSite()` au cookie de déconnexion :

```java
// AVANT
Cookie expiredCookie = Cookie.of("session_id", "").path("/").maxAge(0);

// APRÈS
Cookie expiredCookie = Cookie.of("session_id", "")
    .path("/")
    .maxAge(0)
    .sameSite(Cookie.SameSite.Lax);
```

### 2. plant_shop_java_quarkus_microservices

**Fichier** : `auth-service/src/controllers/AuthController.java`

**Imports à ajouter** (début du fichier) :

```java
import jakarta.ws.rs.core.NewCookie.SameSite;
```

**Ligne ~62** (méthode `login()`) - Modifier la création du cookie :

```java
// AVANT
NewCookie cookie = new NewCookie.Builder("session_id")
    .value(sessionId)
    .path("/")
    .maxAge(3600)
    .httpOnly(true)
    .build();

// APRÈS
NewCookie cookie = new NewCookie.Builder("session_id")
    .value(sessionId)
    .path("/")
    .maxAge(3600)
    .httpOnly(true)
    .sameSite(SameSite.LAX)
    .build();
```

**Ligne ~80** (méthode `logout()`) - Ajouter au cookie de déconnexion :

```java
// AVANT
NewCookie expiredCookie = new NewCookie.Builder("session_id")
    .value("")
    .path("/")
    .maxAge(0)
    .build();

// APRÈS
NewCookie expiredCookie = new NewCookie.Builder("session_id")
    .value("")
    .path("/")
    .maxAge(0)
    .sameSite(SameSite.LAX)
    .build();
```

### 3. plant_shop_java_spring_boot_security_microservices

**Fichier** : `auth-service/src/main/resources/application.properties` (ou créer si absent)

**Ajouter** :

```properties
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
server.servlet.session.cookie.max-age=3600
```

**Alternative** : Si cookies manuels dans `AuthController.java`, modifier les `ResponseCookie` :

```java
// AVANT
ResponseCookie cookie = ResponseCookie.from("session_id", sessionId)
    .path("/")
    .httpOnly(true)
    .maxAge(3600)
    .build();

// APRÈS
ResponseCookie cookie = ResponseCookie.from("session_id", sessionId)
    .path("/")
    .httpOnly(true)
    .maxAge(3600)
    .sameSite("Lax")
    .build();
```

### 4. plant_shop_java_spring_boot_security_hibernate_microservices

**Identique à plant_shop_java_spring_boot_security_microservices** (même framework Spring Boot).

Suivre les mêmes instructions que la section 3 ci-dessus.

### 5. plant_shop_java_javalin_microservices

**✅ DÉJÀ FAIT** - Le fichier `auth-service/src/controllers/AuthController.java` contient déjà :

- Ligne 67 : `cookie.setSameSite(SameSite.LAX);` dans `login()`
- Ligne 85 : `cookie.setSameSite(SameSite.LAX);` dans `logout()`

**Aucune action requise.**

---

### Résumé des actions

| Projet           | Fichier                             | Action                                                | Statut     |
| ---------------- | ----------------------------------- | ----------------------------------------------------- | ---------- |
| Javalin          | auth-service/AuthController.java    | -                                                     | ✅ Déjà OK |
| Micronaut        | auth-service/AuthController.java    | Ajouter `.sameSite(Cookie.SameSite.Lax)`              | ❌ À faire |
| Quarkus          | auth-service/AuthController.java    | Ajouter `.sameSite(SameSite.LAX)`                     | ❌ À faire |
| Spring Boot      | auth-service/application.properties | Ajouter `server.servlet.session.cookie.same-site=lax` | ❌ À faire |
| Spring Hibernate | auth-service/application.properties | Ajouter `server.servlet.session.cookie.same-site=lax` | ❌ À faire |

**Total : 4 fichiers à modifier sur 5 projets**

---

## Débug compilation 1 - Javalin

**Date** : 15 novembre 2025
**Projet** : `plant_shop_java_javalin_microservices`
**Commande** : `make build-dev`

### Problème 1 : Module utils partagé avec ApiMapper et Request

**Erreur** :
```
/utils/src/ApiMapper.java:10: error: package model does not exist
import model.Order;
/utils/src/ApiMapper.java:11: error: package model does not exist
import model.OrderItem;
/utils/src/ApiMapper.java:12: error: package model does not exist
import model.Plant;
/utils/src/ApiMapper.java:13: error: package model does not exist
import model.User;
```

**Cause** : Le module `utils` partagé contenait un `ApiMapper.java` centralisé qui référençait tous les modèles métier (User, Plant, Order, OrderItem), violant le principe d'isolation des microservices.

**Solution** :
- Suppression de `utils/src/ApiMapper.java`
- Suppression de `utils/src/Request.java`
- Chaque service possède déjà sa propre version locale de ces classes

**Fichiers modifiés** :
```bash
rm utils/src/ApiMapper.java
rm utils/src/Request.java
```

### Problème 2 : Makefile référençait les fichiers supprimés

**Erreur** :
```
error: file not found: /utils/src/ApiMapper.java
make: *** [Makefile:45: compile-auth] Error 2
```

**Cause** : Le Makefile tentait de compiler les versions centralisées de `ApiMapper.java` et `Request.java` depuis `utils/src/` pour chaque service.

**Solution** : Modification du Makefile pour compiler les versions locales de chaque service.

**Fichier modifié** : `Makefile`

**Changements** (lignes 42-77) :

```makefile
# AVANT
compile-auth: compile-utils
	@javac -cp "$(SERVICE_CP)" -d $(AUTH_DIR)/bin \
		$(UTILS_DIR)/src/ApiMapper.java \
		$(UTILS_DIR)/src/Request.java \
		$(wildcard $(AUTH_DIR)/src/controllers/*.java)

# APRÈS
compile-auth: compile-utils
	@javac -cp "$(SERVICE_CP)" -d $(AUTH_DIR)/bin \
		$(wildcard $(AUTH_DIR)/src/util/*.java) \
		$(wildcard $(AUTH_DIR)/src/model/*.java) \
		$(wildcard $(AUTH_DIR)/src/repository/*.java) \
		$(wildcard $(AUTH_DIR)/src/security/*.java) \
		$(wildcard $(AUTH_DIR)/src/controllers/*.java)
```

Même modification appliquée pour `compile-catalog`, `compile-order`, et `compile-user`.

### Problème 3 : Dossiers models/ au lieu de model/

**Erreur** :
```
auth-service/src/util/ApiMapper.java:7: error: package model does not exist
import model.User;
```

**Cause** : Discordance entre nom de package (`model`) et nom de dossier (`models/`). Java exige que la structure de dossiers corresponde exactement aux packages.

**Solution** : Renommage des dossiers au pluriel vers le singulier pour correspondre aux packages.

**Fichiers modifiés** :
```bash
# Pour tous les services (auth, catalog, order, user)
mv auth-service/src/models → auth-service/src/model
mv auth-service/src/repositories → auth-service/src/repository
mv catalog-service/src/models → catalog-service/src/model
mv catalog-service/src/repositories → catalog-service/src/repository
mv order-service/src/models → order-service/src/model
mv order-service/src/repositories → order-service/src/repository
mv user-service/src/models → user-service/src/model
mv user-service/src/repositories → user-service/src/repository
```

**Makefile mis à jour** (lignes 55-77) :
```makefile
# AVANT
$(wildcard $(CATALOG_DIR)/src/models/*.java)
$(wildcard $(CATALOG_DIR)/src/repositories/*.java)

# APRÈS
$(wildcard $(CATALOG_DIR)/src/model/*.java)
$(wildcard $(CATALOG_DIR)/src/repository/*.java)
```

### Problème 4 : BaseRepository manquant

**Erreur** (auth-service) :
```
auth-service/src/repository/UserRepository.java:6: error: cannot find symbol
public final class UserRepository extends BaseRepository<User> {
                                          ^
  symbol: class BaseRepository
```

**Cause** : Les repositories héritaient d'une classe abstraite `BaseRepository<T>` qui n'existait pas dans le projet microservices.

**Solution** : Retrait de l'héritage et implémentation directe des méthodes dans chaque repository.

**Fichiers modifiés** :

#### auth-service/src/repository/UserRepository.java

```java
// AVANT
public final class UserRepository extends BaseRepository<User> {
    public UserRepository(Connection db) {
        super(db, "users");
    }

// APRÈS
public final class UserRepository {
    private final Connection db;

    public UserRepository(Connection db) {
        this.db = db;
    }

    public User find(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
                return null;
            }
        }
    }
```

Ajout des méthodes manquantes : `find(int)`, suppression de `@Override` sur `mapFromResultSet()`, changement de visibilité `protected → private`.

#### catalog-service/src/repository/PlantRepository.java

Même refactoring + ajout de :
- `find(int id)`
- `list()`
- `delete(int id)`

#### order-service/src/repository/OrderRepository.java

Même refactoring + ajout de :
- `find(int id)`
- `delete(int id)`

#### order-service/src/repository/OrderItemRepository.java

Même refactoring (pas besoin de `find/list/delete` génériques, déjà des méthodes spécifiques comme `listByOrder()`).

#### user-service/src/repository/UserRepository.java

Même refactoring + ajout de :
- `find(int id)`
- `list()`
- `delete(int id)`

### Problème 5 : Gateway utilise util.Request

**Erreur** :
```
gateway/src/Routes.java:6: error: cannot find symbol
import util.Request;
           ^
  symbol:   class Request
  location: package util

gateway/src/Routes.java:113: error: cannot find symbol
        String sessionId = Request.extractSessionId(ex);
                           ^
  symbol:   variable Request
```

**Cause** : La Gateway importait `util.Request` du module `utils` partagé qui a été supprimé.

**Solution** : Implémentation locale de `extractSessionId()` dans la classe `GatewayHandler`.

**Fichier modifié** : `gateway/src/Routes.java`

```java
// AVANT
import util.Request;
...
String sessionId = Request.extractSessionId(ex);

// APRÈS
import java.util.stream.Stream;
...
String sessionId = extractSessionId(ex);

// Nouvelle méthode privée ajoutée dans GatewayHandler
private static String extractSessionId(HttpExchange ex) {
    String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
    if (cookieHeader == null) {
        return null;
    }

    return Stream.of(cookieHeader.split(";"))
        .map(String::trim)
        .filter(cookie -> cookie.startsWith("session_id="))
        .map(cookie -> cookie.substring("session_id=".length()))
        .findFirst()
        .orElse(null);
}
```

### Résultat final

**Statut** : ✅ Compilation réussie

```bash
make build-dev
🔧 Compilation utilitaires
🔐 Compilation AuthService
🌱 Compilation CatalogService
🧾 Compilation OrderService
👥 Compilation UserService
🚪 Compilation Gateway
```

**Nombre total d'erreurs corrigées** : 35+ erreurs de compilation

**Fichiers modifiés** :
- 1 Makefile
- 8 renommages de dossiers (models→model, repositories→repository)
- 5 repositories refactorisés (UserRepository ×2, PlantRepository, OrderRepository, OrderItemRepository)
- 1 gateway (Routes.java)
- 2 suppressions (utils/ApiMapper.java, utils/Request.java)

---

## Prévision de modifications à faire dans les 4 autres projets - après débuggagecompilation Javalin 1

**Date** : 15 novembre 2025
**Projets auditées** :
1. `plant_shop_java_micronaut_microservices`
2. `plant_shop_java_quarkus_microservices`
3. `plant_shop_java_spring_microservices/plant_shop_java_spring_boot_security_microservices`
4. `plant_shop_java_spring_microservices/plant_shop_java_spring_boot_security_hibernate_microservices`

### Résumé exécutif

Les 4 projets présentent **TOUS les mêmes problèmes** que le projet Javalin :
- ✅ Dossiers `models/` et `repositories/` au pluriel mais packages `model` et `repository` au singulier
- ✅ Module `utils` partagé avec `ApiMapper.java` et `Request.java` référençant tous les modèles métier
- ✅ Classes `BaseRepository<T>` manquantes alors que les repositories en héritent
- ⚠️ Makefiles compilant parfois les versions `utils/` au lieu des versions locales

**Prévision** : Les mêmes corrections seront nécessaires pour les 4 projets.

---

### Problème 1 : Discordance dossiers/packages (IDENTIQUE à Javalin)

#### État actuel

**Tous les 4 projets** :
```bash
# Structure des dossiers (pluriel)
auth-service/src/models/
auth-service/src/repositories/
catalog-service/src/models/
catalog-service/src/repositories/
order-service/src/models/
order-service/src/repositories/
user-service/src/models/
user-service/src/repositories/

# Déclaration des packages (singulier)
package model;
package repository;
```

#### Vérification effectuée

```bash
$ head -1 plant_shop_java_micronaut_microservices/auth-service/src/models/User.java
package model;

$ head -1 plant_shop_java_quarkus_microservices/auth-service/src/models/User.java
package model;

$ head -1 plant_shop_java_spring_boot_security_microservices/auth-service/src/models/User.java
package model;
```

#### Solution à appliquer (IDENTIQUE pour les 4 projets)

**Renommage des dossiers** pour chaque projet :

```bash
# Micronaut
mv auth-service/src/models → auth-service/src/model
mv auth-service/src/repositories → auth-service/src/repository
mv catalog-service/src/models → catalog-service/src/model
mv catalog-service/src/repositories → catalog-service/src/repository
mv order-service/src/models → order-service/src/model
mv order-service/src/repositories → order-service/src/repository
mv user-service/src/models → user-service/src/model
mv user-service/src/repositories → user-service/src/repository

# Même opération pour Quarkus, Spring Boot Security, Spring Boot Hibernate
```

**Mise à jour des Makefiles** :

```makefile
# AVANT (exemple Micronaut ligne 47-48)
$(wildcard $(AUTH_DIR)/src/models/*.java)
$(wildcard $(AUTH_DIR)/src/repositories/*.java)

# APRÈS
$(wildcard $(AUTH_DIR)/src/model/*.java)
$(wildcard $(AUTH_DIR)/src/repository/*.java)
```

---

### Problème 2 : Module utils partagé avec ApiMapper et Request

#### État actuel

**Micronaut, Quarkus, Spring Security, Spring Hibernate** ont tous :

```bash
utils/src/ApiMapper.java  # Référence model.User, model.Plant, model.Order, model.OrderItem
utils/src/Request.java    # Référence model.User et repository.UserRepository
```

Vérification :
```bash
$ grep "import model\." plant_shop_java_micronaut_microservices/utils/src/ApiMapper.java
import model.Order;
import model.OrderItem;
import model.Plant;
import model.User;

$ grep "import model\." plant_shop_java_quarkus_microservices/utils/src/ApiMapper.java
import model.Order;
import model.OrderItem;
import model.Plant;
import model.User;
```

#### ApiMapper locaux déjà existants

**Bonne nouvelle** : Contrairement à Javalin, les 4 projets ont **DÉJÀ** des ApiMapper locaux dans certains services :

**Micronaut** :
- `auth-service/src/util/ApiMapper.java` ✅
- `catalog-service/src/util/ApiMapper.java` ✅
- `user-service/src/util/ApiMapper.java` ✅
- `order-service` : **❌ MANQUANT**

**Quarkus** (identique à Micronaut) :
- `auth-service/src/util/ApiMapper.java` ✅
- `catalog-service/src/util/ApiMapper.java` ✅
- `user-service/src/util/ApiMapper.java` ✅
- `order-service` : **❌ MANQUANT**

**Spring Boot Security** (identique) :
- `auth-service/src/util/ApiMapper.java` ✅
- `catalog-service/src/util/ApiMapper.java` ✅
- `user-service/src/util/ApiMapper.java` ✅
- `order-service` : **❌ MANQUANT**

**Spring Boot Hibernate** (identique) :
- `auth-service/src/util/ApiMapper.java` ✅
- `catalog-service/src/util/ApiMapper.java` ✅
- `user-service/src/util/ApiMapper.java` ✅
- `order-service` : **❌ MANQUANT**

#### Solution à appliquer

**Étape 1** : Suppression des fichiers centralisés

```bash
# Pour chaque projet
rm utils/src/ApiMapper.java
rm utils/src/Request.java
```

**Étape 2** : Créer ApiMapper.java manquant pour order-service

Il faudra créer `order-service/src/util/ApiMapper.java` avec uniquement les méthodes nécessaires :
- `toOrder(Order, List<OrderItem>)`
- `toOrderItem(OrderItem, Plant)`

(Copier depuis le projet Javalin complété)

**Étape 3** : Mettre à jour les UTILS_SRC dans Makefile

```makefile
# Déjà fait dans Micronaut ligne 12 :
UTILS_SRC := $(filter-out $(UTILS_DIR)/src/ApiMapper.java $(UTILS_DIR)/src/Request.java, $(wildcard $(UTILS_DIR)/src/*.java))
```

**Étape 4** : Mettre à jour les règles de compilation

Exemple **Micronaut Makefile ligne 50-58** (compile-catalog) :

```makefile
# AVANT
compile-catalog: compile-utils
	@javac -proc:none -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin \
		$(UTILS_DIR)/src/ApiMapper.java \    ← À SUPPRIMER
		$(UTILS_DIR)/src/Request.java \       ← À SUPPRIMER
		$(wildcard $(CATALOG_DIR)/src/controllers/*.java) \
		$(wildcard $(CATALOG_DIR)/src/models/*.java) \
		$(wildcard $(CATALOG_DIR)/src/repositories/*.java)

# APRÈS
compile-catalog: compile-utils
	@javac -proc:none -cp "$(SERVICE_CP)" -d $(CATALOG_DIR)/bin \
		$(wildcard $(CATALOG_DIR)/src/util/*.java) \
		$(wildcard $(CATALOG_DIR)/src/model/*.java) \
		$(wildcard $(CATALOG_DIR)/src/repository/*.java) \
		$(wildcard $(CATALOG_DIR)/src/security/*.java) \
		$(wildcard $(CATALOG_DIR)/src/controllers/*.java)
```

**Note** : Vérifier toutes les règles `compile-*` dans chaque Makefile.

---

### Problème 3 : BaseRepository manquant

#### État actuel

**Micronaut, Quarkus, Spring Boot Security** :

Tous les repositories héritent de `BaseRepository<T>` qui **n'existe pas** :

```bash
$ grep "extends BaseRepository" plant_shop_java_micronaut_microservices/auth-service/src/repositories/UserRepository.java
public final class UserRepository extends BaseRepository<User> {

$ grep "extends BaseRepository" plant_shop_java_quarkus_microservices/auth-service/src/repositories/UserRepository.java
public class UserRepository extends BaseRepository<User> {

$ grep "extends BaseRepository" plant_shop_java_spring_boot_security_microservices/auth-service/src/repositories/UserRepository.java
public class UserRepository extends BaseRepository<User> {
```

**Spring Boot Hibernate** : Utilise `JpaRepository` (Spring Data) ✅ **PAS DE PROBLÈME**

```bash
$ grep "extends" plant_shop_java_spring_boot_security_hibernate_microservices/auth-service/src/repositories/UserRepository.java
public interface UserRepository extends JpaRepository<User, Integer> {
```

#### Solution à appliquer

**Pour Micronaut, Quarkus, Spring Boot Security** : Même refactoring que Javalin.

**Repositories à modifier par projet** :
- `auth-service/src/repository/UserRepository.java`
- `catalog-service/src/repository/PlantRepository.java`
- `order-service/src/repository/OrderRepository.java`
- `order-service/src/repository/OrderItemRepository.java`
- `user-service/src/repository/UserRepository.java`

**Modifications nécessaires** :

```java
// AVANT
public final class UserRepository extends BaseRepository<User> {
    public UserRepository(Connection db) {
        super(db, "users");
    }

    @Override
    protected User mapFromResultSet(ResultSet rs) throws SQLException {
        // ...
    }

// APRÈS
public final class UserRepository {
    private final Connection db;

    public UserRepository(Connection db) {
        this.db = db;
    }

    public User find(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
                return null;
            }
        }
    }

    public List<User> list() throws SQLException { /* ... */ }
    public void delete(int id) throws SQLException { /* ... */ }

    private User mapFromResultSet(ResultSet rs) throws SQLException {
        // ... (changement protected → private)
    }
```

**Méthodes à ajouter par repository** :

| Repository          | Méthodes à implémenter                 |
| ------------------- | -------------------------------------- |
| UserRepository      | `find(int)`, `list()`, `delete(int)`   |
| PlantRepository     | `find(int)`, `list()`, `delete(int)`   |
| OrderRepository     | `find(int)`, `delete(int)`             |
| OrderItemRepository | (déjà des méthodes spécifiques)        |

---

### Problème 4 : Gateway utilisant util.Request (probable)

#### Prévision

Les gateways des 4 projets utilisent **probablement** `util.Request.extractSessionId()`.

**Vérification à faire** :
```bash
grep -n "import util.Request" */gateway/src/*.java
grep -n "Request\.extract" */gateway/src/*.java
```

#### Solution prévue

Même solution que Javalin :
1. Supprimer `import util.Request`
2. Ajouter `import java.util.stream.Stream`
3. Ajouter méthode locale `extractSessionId()`

---

### Tableau récapitulatif des modifications prévues

| Problème                              | Micronaut | Quarkus | Spring Security | Spring Hibernate | Javalin     |
| ------------------------------------- | --------- | ------- | --------------- | ---------------- | ----------- |
| Renommer `models/` → `model/`         | ✅ Oui    | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Fait     |
| Renommer `repositories/` → `repository/` | ✅ Oui | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Fait     |
| Supprimer `utils/ApiMapper.java`      | ✅ Oui    | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Fait     |
| Supprimer `utils/Request.java`        | ✅ Oui    | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Fait     |
| Créer `order-service/util/ApiMapper`  | ✅ Oui    | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Déjà OK  |
| Refactor repositories (BaseRepository) | ✅ Oui   | ✅ Oui  | ✅ Oui          | ❌ Non (JPA)     | ✅ Fait     |
| Modifier Makefile (models → model)    | ✅ Oui    | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Fait     |
| Modifier Makefile (utils → local)     | ✅ Oui    | ✅ Oui  | ✅ Oui          | ✅ Oui           | ✅ Fait     |
| Gateway : extractSessionId()          | ⚠️ Probable | ⚠️ Probable | ⚠️ Probable | ⚠️ Probable     | ✅ Fait     |

**Légende** :
- ✅ Oui : Modification nécessaire
- ❌ Non : Pas de modification
- ⚠️ Probable : À vérifier
- ✅ Fait : Déjà corrigé dans Javalin

---

### Estimation de l'effort

**Par projet** :
- **Renommages de dossiers** : 8 dossiers × 4 projets = **32 renommages**
- **Suppressions** : 2 fichiers × 4 projets = **8 suppressions**
- **Créations** : 1 ApiMapper (order-service) × 4 projets = **4 créations**
- **Refactoring repositories** : 5 repositories × 3 projets (pas Hibernate) = **15 refactorings**
- **Modifications Makefile** : 1 fichier × 4 projets = **4 modifications**
- **Gateway** : 1 fichier × 4 projets (à vérifier) = **~4 modifications**

**Total estimé** : **67 opérations** (vs 22 pour Javalin)

---

### Stratégie de correction recommandée

1. **Ordre des projets** : Suivre l'ordre initial
   - Micronaut
   - Quarkus
   - Spring Boot Security
   - Spring Boot Hibernate (plus simple car JPA)

2. **Ordre des opérations par projet** :
   1. Renommer dossiers `models/` → `model/` et `repositories/` → `repository/`
   2. Supprimer `utils/ApiMapper.java` et `utils/Request.java`
   3. Créer `order-service/src/util/ApiMapper.java`
   4. Refactorer les 5 repositories (sauf Hibernate)
   5. Mettre à jour le Makefile
   6. Vérifier et corriger la Gateway
   7. **Compiler avec `make build-dev`**
   8. Déboguer erreurs supplémentaires éventuelles

3. **Instructions techniques détaillées par projet** :

### Instructions pour Micronaut

#### ApiMapper order-service

**Fichier à créer** : `plant_shop_java_micronaut_microservices/order-service/src/util/ApiMapper.java`

**Copie directe** : ✅ Copier depuis `plant_shop_java_javalin_microservices/order-service/src/util/ApiMapper.java`

**Raison** : Java pur, aucune dépendance framework. Le code est identique.

#### Repositories (5 fichiers)

**Annotations Micronaut à conserver** :
- `@Singleton` sur la classe (ligne 8)
- `Connection db` injecté via constructeur

**Exemple UserRepository** :

```java
package repository;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.User;

@Singleton  // ← CONSERVER cette annotation Micronaut
public final class UserRepository {

    private final Connection db;

    public UserRepository(Connection db) {  // ← CONSERVER injection par constructeur
        this.db = db;
    }

    public User find(int id) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    public List<User> list() throws SQLException {
        // ... (même implémentation que Javalin)
    }

    public void delete(int id) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    private User mapFromResultSet(ResultSet rs) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    // Méthodes spécifiques existantes à conserver
    public User findByEmailWithPassword(String email) throws SQLException {
        // ... (garder tel quel)
    }

    public int create(User u) throws SQLException {
        // ... (garder tel quel)
    }

    public void update(User u) throws SQLException {
        // ... (garder tel quel)
    }
}
```

**Modifications à appliquer** :
- Retirer `extends BaseRepository<User>`
- Ajouter `private final Connection db;`
- Ajouter méthodes `find()`, `list()`, `delete()` (copier depuis Javalin)
- Changer `protected User mapFromResultSet()` → `private User mapFromResultSet()`
- **CONSERVER** `@Singleton`
- **CONSERVER** toutes les méthodes spécifiques existantes

**Fichiers à modifier** :
1. `auth-service/src/repository/UserRepository.java` (+ `@Singleton`)
2. `catalog-service/src/repository/PlantRepository.java` (+ `@Singleton`)
3. `order-service/src/repository/OrderRepository.java` (+ `@Singleton`)
4. `order-service/src/repository/OrderItemRepository.java` (+ `@Singleton`)
5. `user-service/src/repository/UserRepository.java` (+ `@Singleton`)

#### Gateway

**Fichier** : `gateway/src/Routes.java`

**Ligne 6** : `import util.Request;` → SUPPRIMER

**Ligne 6** : Ajouter `import java.util.stream.Stream;`

**Ligne 113** : `Request.extractSessionId(ex)` → `extractSessionId(ex)`

**Ajouter la méthode** (copie exacte depuis Javalin) :

```java
private static String extractSessionId(HttpExchange ex) {
    String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
    if (cookieHeader == null) {
        return null;
    }

    return Stream.of(cookieHeader.split(";"))
        .map(String::trim)
        .filter(cookie -> cookie.startsWith("session_id="))
        .map(cookie -> cookie.substring("session_id=".length()))
        .findFirst()
        .orElse(null);
}
```

**Vérification** : Micronaut utilise `HttpExchange` (identique à Javalin) → pas d'adaptation nécessaire.

---

### Instructions pour Quarkus

#### ApiMapper order-service

**Fichier à créer** : `plant_shop_java_quarkus_microservices/order-service/src/util/ApiMapper.java`

**Copie directe** : ✅ Copier depuis `plant_shop_java_javalin_microservices/order-service/src/util/ApiMapper.java`

**Raison** : Java pur, aucune dépendance framework. Le code est identique.

#### Repositories (5 fichiers)

**Annotations Quarkus à conserver** :
- `@RequestScoped` sur la classe
- `@Inject` sur le constructeur
- `Connection db` injecté via constructeur

**Exemple UserRepository** :

```java
package repository;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.User;

@RequestScoped  // ← CONSERVER cette annotation Quarkus
public class UserRepository {

    private final Connection db;

    @Inject  // ← CONSERVER cette annotation Quarkus
    public UserRepository(Connection db) {
        this.db = db;
    }

    public User find(int id) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    public List<User> list() throws SQLException {
        // ... (même implémentation que Javalin)
    }

    public void delete(int id) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    private User mapFromResultSet(ResultSet rs) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    // Méthodes spécifiques existantes à conserver
    // ...
}
```

**Modifications à appliquer** :
- Retirer `extends BaseRepository<User>`
- Ajouter `private final Connection db;`
- Ajouter méthodes `find()`, `list()`, `delete()` (copier depuis Javalin)
- Changer `protected User mapFromResultSet()` → `private User mapFromResultSet()`
- **CONSERVER** `@RequestScoped`
- **CONSERVER** `@Inject` sur le constructeur
- **CONSERVER** toutes les méthodes spécifiques existantes

**Fichiers à modifier** :
1. `auth-service/src/repository/UserRepository.java` (+ `@RequestScoped` + `@Inject`)
2. `catalog-service/src/repository/PlantRepository.java` (+ `@RequestScoped` + `@Inject`)
3. `order-service/src/repository/OrderRepository.java` (+ `@RequestScoped` + `@Inject`)
4. `order-service/src/repository/OrderItemRepository.java` (+ `@RequestScoped` + `@Inject`)
5. `user-service/src/repository/UserRepository.java` (+ `@RequestScoped` + `@Inject`)

#### Gateway

**Identique à Micronaut** : Quarkus utilise aussi `HttpExchange`.

Copier la même solution que Micronaut.

---

### Instructions pour Spring Boot Security

#### ApiMapper order-service

**Fichier à créer** : `plant_shop_java_spring_boot_security_microservices/order-service/src/util/ApiMapper.java`

**Copie directe** : ✅ Copier depuis `plant_shop_java_javalin_microservices/order-service/src/util/ApiMapper.java`

**Raison** : Java pur, aucune dépendance framework. Le code est identique.

#### Repositories (5 fichiers)

**Annotations Spring à conserver** :
- `@Repository` sur la classe
- `@RequestScope` sur la classe
- `@Autowired` sur le constructeur
- `Connection db` injecté via constructeur

**Exemple UserRepository** :

```java
package repository;

import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository      // ← CONSERVER cette annotation Spring
@RequestScope    // ← CONSERVER cette annotation Spring
public class UserRepository {

    private final Connection db;

    @Autowired  // ← CONSERVER cette annotation Spring
    public UserRepository(Connection db) {
        this.db = db;
    }

    public User find(int id) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    public List<User> list() throws SQLException {
        // ... (même implémentation que Javalin)
    }

    public void delete(int id) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    private User mapFromResultSet(ResultSet rs) throws SQLException {
        // ... (même implémentation que Javalin)
    }

    // Méthodes spécifiques existantes à conserver
    // ...
}
```

**Modifications à appliquer** :
- Retirer `extends BaseRepository<User>`
- Ajouter `private final Connection db;`
- Ajouter méthodes `find()`, `list()`, `delete()` (copier depuis Javalin)
- Changer `protected User mapFromResultSet()` → `private User mapFromResultSet()`
- **CONSERVER** `@Repository` et `@RequestScope`
- **CONSERVER** `@Autowired` sur le constructeur
- **CONSERVER** toutes les méthodes spécifiques existantes

**Fichiers à modifier** :
1. `auth-service/src/repository/UserRepository.java` (+ `@Repository` + `@RequestScope` + `@Autowired`)
2. `catalog-service/src/repository/PlantRepository.java` (+ `@Repository` + `@RequestScope` + `@Autowired`)
3. `order-service/src/repository/OrderRepository.java` (+ `@Repository` + `@RequestScope` + `@Autowired`)
4. `order-service/src/repository/OrderItemRepository.java` (+ `@Repository` + `@RequestScope` + `@Autowired`)
5. `user-service/src/repository/UserRepository.java` (+ `@Repository` + `@RequestScope` + `@Autowired`)

#### Gateway

**Identique à Micronaut et Quarkus** : Spring Boot Security utilise aussi `HttpExchange`.

Copier la même solution.

---

### Instructions pour Spring Boot Hibernate

#### ApiMapper order-service

**Fichier à créer** : `plant_shop_java_spring_boot_security_hibernate_microservices/order-service/src/util/ApiMapper.java`

**Copie directe** : ✅ Copier depuis `plant_shop_java_javalin_microservices/order-service/src/util/ApiMapper.java`

**Raison** : Java pur, aucune dépendance framework. Le code est identique.

#### Repositories

**❌ PAS DE REFACTORING NÉCESSAIRE**

**Raison** : Spring Boot Hibernate utilise `JpaRepository` (Spring Data), pas de `BaseRepository` personnalisé.

**Vérification effectuée** :

```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

→ Les repositories sont des **interfaces** étendant `JpaRepository` (fourni par Spring Data JPA).

→ **Aucune modification nécessaire**.

#### Gateway

**Identique aux 3 autres** : Utilise `HttpExchange`.

Copier la même solution que Micronaut, Quarkus et Spring Boot Security.
