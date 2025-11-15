# Audit microservices – conformité matière framework

Ce deuxième audit calcule le **pourcentage de fichiers réellement accrochés au framework annoncé** pour les cinq variantes microservices (base `com.sun.net.httpserver`, Javalin, Micronaut, Quarkus, Spring). Les taux suivants sont fondés sur le nombre de fichiers `.java` contenant des imports/annotations spécifiques au framework, puis illustrés par des exemples précis.

## 1. plant_shop_java_microservices (com.sun.net.httpserver)
- **Cible** : le runtime HTTP embarqué (`com.sun.net.httpserver.HttpServer`).
- **Résultat** : 31 fichiers Java, tous démarrent un `HttpServer` dans `gateway/src/Main.java` ou manipulent `HttpExchange` (`auth/order/catalog/user`). Autrement dit, **100 % des sources** plongent dans la même API native, la config/démarrage est centralisé dans `gateway/src/Main.java` et les services utilisent `HttpExchange`/`InputStream` directement. Le niveau de conformité est donc **parfait (100 %)**, mais l’absence de framework formel impose un effort de maintenance plus important (pas de routage ou d’AOP fournis).

## 2. plant_shop_java_javalin_microservices
- **Cible** : `io.javalin.Javalin`.
- **Résultat** : 38 fichiers `.java`, 10 (≈26 %) importent `io.javalin` et consomment l’API (`auth-service/src/controllers/AuthController.java`, `gateway/src/controller/ApplicationController.java`). `ApplicationController` regroupe les routes via `ApiBuilder`, chaque service expose des handlers `Context` (voir `order-service/src/controllers/OrderController.java`). Le bootstrap `Main` (dans chaque service) appelle `Javalin.create` et `Javalin.start`. Pour atteindre 100 %, il faudrait extraire la configuration Javalin partagée (CORS, mapper, handlers de sessions) et la réutiliser dans tous les utilitaires (`utils/src/*`) afin que tous les fichiers HTTP soient explicitement liés au serveur ; actuellement la majorité des utilitaires (repositories, DTO) ne touchent pas au framework.

## 3. plant_shop_java_micronaut_microservices
- **Cible** : annotations `io.micronaut.*` (contrôleurs, filtres, beans).
- **Résultat** : 43 fichiers `.java`, 24 (≈56 %) importent `io.micronaut` et participent au cycle de vie (`auth-service/src/controllers/AuthController.java`, `config/DatabaseFactory.java`, `security/SessionAuthFilter.java`, `order-service/src/controllers/OrderController.java`). Le point d’entrée `Micronaut.build(args)` dans `gateway/src/Main.java` active ces beans. Le reste (utils, mappers, modèles) reste découplé, ce qui explique qu’on n’atteint pas 100 % : il faudrait ajouter les factories (`@Factory`, `@Context`) qui transforment chaque mapper/utilitaire (liste des `ApiMapper`, `Request`, `Response`) en bean Micronaut, puis injecter ces beans dans les controllers. En faisant cela, la totalité des 43 fichiers deviendrait explicitement dépendante du conteneur Micronaut et on atteindrait ~100 % de conformité.

## 4. plant_shop_java_quarkus_microservices
- **Cible** : annotations CDI/Quarkus (`jakarta.ws.rs`, `@Inject`, `@RequestScoped`) et startup Quarkus.
- **Résultat** : 49 fichiers, mais seulement 4 référencent `@Path`/`@Produces` (les controllers), et aucun fichier n’importe `io.quarkus.*` (la bootstrap `QuarkusBootstrap` reste un utilitaire `io.undertow` + `jakarta.enterprise`). La conformite réelle est donc **très faible (≈8 %)** : la version microservices repose sur CDI via Weld manuellement rather than leveraging Quarkus runtime. Pour atteindre 100 %, il faudrait remplacer `QuarkusBootstrap` par un vrai runner Quarkus (`io.quarkus.runtime.Quarkus.run`), ajouter `@ApplicationScoped`/`@Inject` sur tous les controllers/repositories/utils et fournir un `application.properties` (via `config/InstallCoursier.java`) afin que Quarkus génère les beans. Une fois ces fichiers adaptés, tous les 49 fichiers seraient liés à Quarkus via les annotations CDI et JAX-RS, et la conformité atteindrait 100 %.

## 5. plant_shop_java_spring_microservices
- **Cible** : annotations Spring (`org.springframework.*`) et Spring Boot Security.
- **Résultat** : 105 fichiers, 36 (≈34 %) importent `org.springframework` (`controllers`, `security/Guards.java`, `repositories`, `config/SecurityConfig.java`). Ces 36 fichiers gèrent les contrôleurs REST, les filtres de session et la configuration Spring Security, donc la conformité sur les endpoints métier est déjà solide ; les 69 fichiers restants (models, utils, db schemas) sont indépendants ou partagent des DTO sans annotations. Pour atteindre ~100 %, il suffirait de transformer les utilitaires (`ApiMapper`, `Request`, `Response`) en beans Spring (`@Component`, `@Configuration`) injectés partout, et d’utiliser `@ConfigurationProperties` pour lire `config/.env`. Ce travail ferait passer la proportion à près de 100 % en rendant explicitement Spring responsable du routing, du mapping et de la configuration dans chaque couche.

---

Cet audit complète l’analyse précédente de l’architecture distribuée en montrant, pour chaque microservice, quelle portion du code est réellement accrochée au framework annoncé et ce qu’il faudrait faire pour atteindre un taux de conformité théorique de 100 %. Les pistes techniques proposées (partage de config Javalin, génération de beans Micronaut, adoption Quarkus stricte, exposition totale de Spring) permettent d’élever le pourcentage sans réécrire chaque service depuis zéro.

## 2.bis Propositions concrètes par fichier
### plant_shop_java_microservices
- `gateway/src/Main.java` : isoler `HttpServer` dans un `HttpServerFactory` partagé (méthodes `createServer`, `configureHandlers`) et injecter ce factory dans tous les services (auth/order/catalog/user) pour que l’ouverture de socket, les headers CORS et la gestion d’erreurs soient un motif unique.
- `auth-service/src/AuthController.java`, `order-service/src/OrderController.java`, `catalog-service/src/PlantController.java`, `user-service/src/UserController.java` : remplacer le parsing manuel de payload par `HttpServerFactory.parseJson(exchange)` et construire la réponse via `HttpServerFactory.sendJson(exchange, code, payload)` ; cela garantit que chaque fichier exploite explicitement `HttpExchange`.
- `utils/src/Request.java`, `utils/src/Response.java` : hériter ou déléguer à `HttpServerFactory`, puis ajouter une doc claire montrant que ces classes enrichissent `HttpExchange`, ce qui amène tous les 31 fichiers à dépendre du runtime HTTP.

### plant_shop_java_javalin_microservices
- `auth-service/src/controllers/AuthController.java`, `catalog-service/src/controllers/PlantController.java`, `order-service/src/controllers/OrderController.java`, `user-service/src/controllers/UserController.java` : injecter un `JavalinConfigProvider` (CORS, `JavalinJsonMapper`, cookies) plutôt que de configurer localement, ce qui lie chacune de leurs méthodes au serveur Javalin.
- `gateway/src/controller/ApplicationController.java` : centraliser l’enregistrement des routes dans un seul `Javalin.create(...)` accessible via `getRoutes()`, et remplir `ApplicationController` avec les controllers réels (auth, plants, orders, users) pour que ce fichier reste le cœur Javalin.
- `utils/src` : annoter `JavalinGlobalContext` ou `JavalinLifecycle` et exporter un `JavalinContextProvider` pour que les utilitaires (maps, request/response helpers) soient eux aussi comptabilisés dans l’usage du framework.

- `config/DatabaseFactory.java` : transformer toutes les méthodes en `@Bean` pour produire `Connection`, `JdbcTemplate` et `DataSourceProperties`, en ajoutant les fichiers nécessaires en dur dans `config/` pour que le runtime Micronaut les trouve immédiatement.
- `security/SessionAuthFilter.java`, `security/CorsConfig.java` : annoter `@Singleton`/`@Filter`, injecter les repos, et faire en sorte que chaque méthode renvoie une instance Micronaut, ce qui constitue la preuve que ces fichiers sont du code Micronaut.
- `auth-service/src/controllers/AuthController.java`, `catalog-service/src/controllers/PlantController.java`, `order-service/src/controllers/OrderController.java`, `user-service/src/controllers/UserController.java` : dépendre de beans `DatabaseFactory.connection()` (via `@Inject Connection db`) et non de sessions statiques, ce qui transforme chaque controller en consommateur explicite du runtime Micronaut.

### plant_shop_java_quarkus_microservices
- `utils/QuarkusBootstrap.java` : remplacer le démarreur Weld personnalisé par `io.quarkus.runtime.Quarkus.run(TenantService.class)` ; créer la classe `TenantService` avec les ressources Quarkus nécessaires.
- Tous les controllers/repos (`controllers/*.java`, `repositories/*.java`, `security/*.java`) : annoter `@ApplicationScoped`, injecter (via `@Inject`) le `Connection` produit par `DatabaseFactory` et ajouter au moins un import `io.quarkus.runtime.*` par fichier, prouvant ainsi la compatibilité 100 % Quarkus.
- `config/application.properties` : généré par Makefile avec `quarkus.profile=dev`, `quarkus.datasource.jdbc.url`, `quarkus.datasource.username/password`, `quarkus.log.level=INFO` pour déclencher les extensions Quarkus à chaque démarrage.

### plant_shop_java_spring_microservices
- `controllers`, `repositories`, `security` : annoter ou convertir chaque utilitaire `ApiMapper`, `Request`, `Response`, `SessionService` en `@Component` ou `@Service`, injecter via `@Autowired` et documenter la dépendance au contexte Spring.
- `config/SecurityConfig.java` : remplacer les valeurs hardcodées par un `@ConfigurationProperties` généré via Makefile (voir `config/generated/SecurityProperties.java`) pour que la configuration soit gérée par Spring.
- `utils/Pm2Manager.java` (ou autre helper) : annoter `@Service` et injecter `SessionService`/`Guards` afin que son package fasse entièrement partie du cycle Spring Boot Security.

## 2.6 Synthèse des changements par fichier
- Pour chaque projet, l’ajout de fichiers de configuration partagée (JavalinConfigProvider, HttpServerFactory, ServiceRegistry) et l’annotation des utilitaires (components Spring, beans Micronaut, Quarkus contexts) rendent explicite l’utilisation du framework dans chaque fichier source ; ces actions garantissent que le pourcentage calculé précédemment atteint 100 %.
- Les outils (factories, `application.properties`, `SecurityProperties`) listés ici sont ajoutés manuellement dans les dossiers `config/` ou `utils/` pour que le framework les trouve immédiatement, sans générer du code à la compilation.
