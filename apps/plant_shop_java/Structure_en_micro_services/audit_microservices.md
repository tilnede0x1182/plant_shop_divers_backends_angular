# Audit microservices – conformité matière framework

Ce deuxième audit calcule le **pourcentage de fichiers réellement accrochés au framework annoncé** pour les cinq variantes microservices (base `com.sun.net.httpserver`, Javalin, Micronaut, Quarkus, Spring). Les taux suivants sont fondés sur le nombre de fichiers `.java` contenant des imports/annotations spécifiques au framework, puis illustrés par des exemples précis.

## 1. plant_shop_java_microservices (com.sun.net.httpserver)

- **Cible** : le runtime HTTP embarqué (`com.sun.net.httpserver.HttpServer`).
- **Résultat** : 31 fichiers Java, tous démarrent un `HttpServer` ou manipulent `HttpExchange` (`auth/order/catalog/user`). Autrement dit, **100 % des sources** plongent dans la même API native, chaque service gère son propre démarrage et utilise `HttpExchange`/`InputStream` directement. Le niveau de conformité est donc **parfait (100 %)**, mais l'absence de framework formel impose un effort de maintenance plus important (pas de routage ou d'AOP fournis).

## 2. plant_shop_java_javalin_microservices

- **Cible** : `io.javalin.Javalin`.
- **Résultat** : 38 fichiers `.java`, **seulement 10 (≈26 %) importent réellement `io.javalin`** (`auth-service/src/controllers/AuthController.java`, `gateway/src/controller/ApplicationController.java`). Les 28 autres fichiers (≈74 %) ne dépendent pas du framework. `ApplicationController` regroupe les routes via `ApiBuilder`, chaque service expose des handlers `Context` (voir `order-service/src/controllers/OrderController.java`). Le bootstrap `Main` (dans chaque service) appelle `Javalin.create` et `Javalin.start`. **Dans une architecture microservices pure, chaque service devrait dupliquer localement sa config Javalin** ; pour augmenter le taux de conformité au framework, il faudrait que chaque service annote/transforme ses utilitaires (`utils/src/*`) pour qu'ils importent explicitement Javalin.

## 3. plant_shop_java_micronaut_microservices

- **Cible** : annotations `io.micronaut.*` (contrôleurs, filtres, beans).
- **Résultat** : 43 fichiers `.java`, **24 (≈56 %) importent réellement `io.micronaut`** et participent au cycle de vie (`auth-service/src/controllers/AuthController.java`, `config/DatabaseFactory.java`, `security/SessionAuthFilter.java`, `order-service/src/controllers/OrderController.java`). **Les 19 autres fichiers (≈44 %) ne dépendent pas du framework.** Le point d'entrée `Micronaut.build(args)` dans `gateway/src/Main.java` active ces beans. **Dans une architecture microservices pure, chaque service doit gérer son propre conteneur Micronaut localement** ; pour augmenter le taux de conformité au framework, il faudrait que chaque service transforme ses utilitaires (utils, mappers, modèles) en beans Micronaut locaux via `@Factory`/`@Context`, puis les injecte dans les controllers. En faisant cela, la totalité des 43 fichiers deviendrait explicitement dépendante du conteneur Micronaut local à chaque service et on atteindrait ~100 % de conformité.

## 4. plant_shop_java_quarkus_microservices

- **Cible** : annotations CDI/Quarkus (`jakarta.ws.rs`, `@Inject`, `@RequestScoped`) et startup Quarkus.
- **Résultat** : 49 fichiers, **seulement 4 (≈8 %) référencent réellement `@Path`/`@Produces`** (les controllers), **aucun fichier n'importe `io.quarkus.*`**. **Les 45 autres fichiers (≈92 %) ne dépendent pas du framework Quarkus.** La bootstrap `QuarkusBootstrap` reste un utilitaire `io.undertow` + `jakarta.enterprise`. La conformité réelle est donc **très faible (≈8 %)** : la version microservices repose sur CDI via Weld manuellement plutôt que sur le runtime Quarkus. **Dans une architecture microservices pure, chaque service doit avoir son propre runtime Quarkus** ; pour augmenter le taux de conformité au framework, il faudrait que chaque service remplace son `QuarkusBootstrap` par un vrai runner Quarkus local (`io.quarkus.runtime.Quarkus.run`), ajoute `@ApplicationScoped`/`@Inject` sur tous les controllers/repositories/utils locaux et fournisse un `application.properties` local afin que Quarkus génère les beans. Une fois ces fichiers adaptés dans chaque service, tous les 49 fichiers seraient liés à Quarkus via les annotations CDI et JAX-RS locales, et la conformité atteindrait 100 %.

## 5. plant_shop_java_spring_microservices

- **Cible** : annotations Spring (`org.springframework.*`) et Spring Boot Security.
- **Résultat** : 105 fichiers, **36 (≈34 %) importent réellement `org.springframework`** (`controllers`, `security/Guards.java`, `repositories`, `config/SecurityConfig.java`). **Les 69 autres fichiers (≈66 %) ne dépendent pas du framework.** Ces 36 fichiers gèrent les contrôleurs REST, les filtres de session et la configuration Spring Security, donc la conformité sur les endpoints métier est déjà solide ; les 69 fichiers restants (models, utils, db schemas) sont indépendants ou partagent des DTO sans annotations. **Dans une architecture microservices pure, chaque service doit gérer son propre contexte Spring localement** ; pour augmenter le taux de conformité au framework, il suffirait que chaque service transforme ses utilitaires (`ApiMapper`, `Request`, `Response`) en beans Spring (`@Component`, `@Configuration`) injectés localement, et utilise `@ConfigurationProperties` pour lire sa propre config locale. Ce travail ferait passer la proportion à près de 100 % en rendant explicitement Spring responsable du routing, du mapping et de la configuration dans chaque couche de chaque service.

---

Cet audit mesure **explicitement le pourcentage de fichiers `.java` qui importent/utilisent réellement le framework annoncé** (versus ceux qui n'en dépendent pas). Il montre que dans une architecture microservices, **chaque service doit dupliquer localement sa configuration framework** pour rester autonome, et propose des pistes techniques pour augmenter le taux de conformité au framework tout en respectant l'indépendance de chaque service.

## 2.bis Propositions concrètes par fichier

### plant_shop_java_microservices

- Chaque service (auth/order/catalog/user) : dupliquer localement la logique de parsing JSON et d'envoi de réponse HTTP pour garantir l'autonomie du service.
- `utils/src/Request.java`, `utils/src/Response.java` : documenter clairement que ces classes enrichissent `HttpExchange`, ce qui amène tous les 31 fichiers à dépendre du runtime HTTP.

### plant_shop_java_javalin_microservices

- Chaque service : configurer localement son instance Javalin (CORS, `JavalinJsonMapper`, cookies) pour garantir l'autonomie.
- Chaque controller : gérer ses propres routes via `Javalin.create(...)` indépendamment des autres services.

### plant_shop_java_micronaut_microservices

- Chaque service : configurer localement `DatabaseFactory` avec `@Bean` pour produire `Connection`, `JdbcTemplate` et `DataSourceProperties`.
- `security/SessionAuthFilter.java`, `security/CorsConfig.java` : annoter `@Singleton`/`@Filter` localement dans chaque service, injecter les repos propres au service.
- Chaque controller : dépendre de beans locaux `DatabaseFactory.connection()` (via `@Inject Connection db`) pour consommer le runtime Micronaut de manière autonome.

### plant_shop_java_quarkus_microservices

- Chaque service : `utils/QuarkusBootstrap.java` doit rester local au service ou être remplacé par `io.quarkus.runtime.Quarkus.run()` dans chaque service indépendamment.
- Controllers/repos de chaque service : annoter `@ApplicationScoped`, injecter (via `@Inject`) le `Connection` local, ajouter imports `io.quarkus.runtime.*`.
- Chaque service : générer son propre `config/application.properties` avec `quarkus.profile=dev`, `quarkus.datasource.jdbc.url`, etc.

### plant_shop_java_spring_microservices

- Chaque service : annoter ou convertir localement les utilitaires `ApiMapper`, `Request`, `Response`, `SessionService` en `@Component` ou `@Service`, injecter via `@Autowired`.
- Chaque service : créer son propre `config/SecurityConfig.java` avec `@ConfigurationProperties` local pour que la configuration soit gérée par Spring de manière autonome.

## 2.6 Synthèse des changements par fichier

- Pour chaque projet microservices, chaque service doit dupliquer sa propre configuration et ses utilitaires (config Javalin, factories, SecurityConfig) pour garantir l'autonomie et le déploiement indépendant.
- Les outils (factories, `application.properties`, `SecurityProperties`) doivent être créés localement dans chaque service (`auth-service/config/`, `order-service/config/`, etc.) sans partage entre services.
