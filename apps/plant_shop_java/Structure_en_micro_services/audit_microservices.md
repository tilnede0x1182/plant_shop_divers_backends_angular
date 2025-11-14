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
- Les appels HTTP vers `/internal/plants/{id}/stock` ne sont jamais atomiques : aucun pattern saga/compensation n’est implémenté, donc les systèmes se désynchronisent dès qu’une mise à jour échoue à mi-parcours.
- Le stockage de session en RAM reste la principale faiblesse : pas de TTL/rotation/révocation centralisée, donc impossible de gérer les déconnexions forcées ou la réplication multi-nœuds.
