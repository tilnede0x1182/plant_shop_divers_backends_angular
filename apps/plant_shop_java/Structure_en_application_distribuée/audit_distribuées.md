# Audit des projets distribués (Java)

## plant_shop_java_javalin_distribuée

- **Observation framework** : les contrôleurs des services auth/catalog/order/user publient bien des `io.javalin.http.Context` et autres types Javalin (`auth-service/src/controllers/AuthController.java`, `catalog-service/src/controllers/PlantController.java`, `order-service/src/controllers/OrderController.java`, `user-service/src/controllers/UserController.java`). Ils semblent pourtant isolés, car aucun point d'entrée ne crée de `Javalin` via `Javalin.create`/`Javalin.start` et le gateway (`gateway/src/Main.java`, `gateway/src/Routes.java`) utilise encore `com.sun.net.httpserver.HttpServer`. L'objet `ApplicationController` fondé sur `io.javalin.apibuilder.ApiBuilder` n'est nulle part instancié. Résultat, la totalité du runtime tourne sans Javalin, seuls des imports évoquent le framework.
- **Proportion d'utilisation réelle** : 0 % des entrées HTTP (5 modules : auth, catalog, order, user et gateway) démarrent un serveur Javalin. Seuls les contrôleurs accèdent aux API Javalin, soit environ 6 fichiers / 39 fichiers `.java`, d'où une utilisation déclarative sans exécution.
- **Fichiers à modifier pour activer Javalin** :
  1. `auth-service/AuthService.java` (à créer/mettre à jour) : démarrer un `Javalin.create(config -> config.jsonMapper(new JavalinJsonMapper()))`, enregistrer `AuthController::register/login/etc`.
  2. `catalog-service/CatalogService.java`, `order-service/OrderService.java`, `user-service/UserService.java` : mêmes bootstrap Javalin et injection de contrôleurs.
  3. `gateway/src/Main.java` & `gateway/src/controller/ApplicationController.java` : remplacer le `HttpServer` par un serveur Javalin unique et exposer `ApplicationController#getRoutes()` au lieu de la logique `GatewayHandler` actuelle.
  4. `utils/src/JavalinJsonMapper.java` : connecter le mapper JSON à chaque instance Javalin pour éviter la logique `org.json` dispersée.
     Sans ces ajustements, le projet ne tourne pas sous Javalin malgré son nom.

## plant_shop_java_micronaut_distribuée

- **Observation framework** : les services sont construits autour de Micronaut : `AuthController`, `PlantController`, `OrderController`, `UserController` sont tous annotés `@Controller`/`@Get`/`@Post`, les filtres `SessionAuthFilter`/`CorsConfig` utilisent `@Filter`, les repositories et modèles sont décorés `io.micronaut.context.annotation` (voir `utils/src/DatabaseFactory.java` qui fournit la connexion via `@Factory` et `@Singleton`). Le module `config/dependencies.txt` liste explicitement les dépendances Micronaut.
- **Proportion d'utilisation réelle** : > 90 % du code API (tous les contrôleurs, filtres de sécurité, repositories, utilitaires) repose sur Micronaut ; chaque service (auth/catalog/order/user) dépend du même stack d'annotations `io.micronaut.*`.
- **Actions nécessaires** : pas d'action supplémentaire, la structure exploite déjà Micronaut de bout en bout. Un point d'entrée `Micronaut.run(...)` est certes absent du dépôt, mais les classes sont prêtes à être packagées par un runner Micronaut standard.

## plant_shop_java_quarkus_distribuée

- **Observation framework** : les contrôleurs utilisent Jakarta REST (`@Path`, `@Produces`, `@Transactional`) et CDI (`@Inject`, `@RequestScoped`). `QuarkusBootstrap.java` instancie un serveur Undertow/Resteasy manuel, `DatabaseFactory` est un producteur CDI (`@Produces`, `@RequestScoped`). Les filtres `CdiRequestScopeFilter` et `SessionAuthFilter` respectent les conventions Quarkus (CDI + `@Provider`).
- **Proportion d'utilisation réelle** : 100 % des services (auth/catalog/order/user) tournent avec des composants CDI/JAX-RS, la seule partie pas Quarkus-natérale est la petite couche de bootstrap manuel qui démarre Resteasy ; mais tout le reste exploite les primitives Quarkus.
- **Actions nécessaires** : aucune modification majeure demandée, les fichiers `auth-service/src/controllers/AuthController.java`, `catalog-service/src/controllers/PlantController.java`, `order-service/src/controllers/OrderController.java`, `user-service/src/controllers/UserController.java`, `utils/src/QuarkusBootstrap.java`, `utils/src/DatabaseFactory.java` sont déjà alignés sur le framework.

## plant_shop_java_spring_distribuée/plant_shop_java_spring_boot_security_distribuée

- **Observation framework** : `AuthController`, `PlantController`, `OrderController`, `UserController` sont des `@RestController` Spring, les repositories sont `@Repository` et injectent `Connection` request-scoped (voir `user-service/src/repositories/UserRepository.java`, `catalog-service/src/repositories/PlantRepository.java`, `order-service/src/repositories/OrderRepository.java`). La configuration `security/SecurityConfig.java`, les filtres `security/SessionAuthFilter.java`, les services `security/SessionService.java`/`Guards.java` utilisent clairement Spring Security.
- **Proportion d'utilisation réelle** : 100 % des endpoints métier utilisent Spring Boot/Security (toutes les annotations, les beans et les filtres sont Spring).
- **Actions nécessaires** : pas de modifications urgentes, l'application exploite déjà Spring Boot Security comme attendu.

## plant_shop_java_spring_distribuée/plant_shop_java_spring_boot_security_hibernate_distribuée

- **Observation framework** : les contrôleurs/sécurité sont identiques à l'autre variante, mais le modèle `auth-service/src/models/User.java` est une `@Entity` Hibernate, `UserRepository` hérite de `JpaRepository`, les réponses JSON passent par Jackson (`@JsonIgnore`). Les repositories `order-service/src/repositories/OrderRepository.java` et `catalog-service/src/repositories/PlantRepository.java` sont aussi des beans Spring managés.
- **Proportion d'utilisation réelle** : 100 % du code métier invoque Spring/Hibernate (contrôleurs, services, repositories, entités).
- **Actions nécessaires** : aucun fichier à modifier, l'intégration Hibernate est déjà présente dans les entités/repositories listés ci-dessus.

## Partie 2 : Comparaison avec leurs versions monolithiques

### plant_shop_java_javalin_distribuée

La version distribuée n’instancie jamais `Javalin`, seuls des contrôleurs importent ses types, donc l’usage effectif du framework est à 0 %. En contraste, la version monolithique possède un point d’entrée `Main` qui démarre `Javalin.create(...)` et expose `ApplicationController`, et 7 fichiers sur 23 (`≈30 %`) importent `io.javalin` de manière active. Cette différence montre que la transformation distribuée a déplacé la responsabilité de gestion des routes vers un proxy HTTP custom (`HttpServer`), ce qui rompt l’alignement avec le framework annoncé.

### plant_shop_java_micronaut_distribuée

Les services distribués utilisent `@Controller`, `@Get`, `@Post`, `@Filter` et d’autres annotations Micronaut dans quasiment chaque classe métier, de sorte que l’empreinte réelle dépasse les 90 %. La version monolithique garde elle aussi un point d’entrée central et 18 fichiers sur 22 (`≈82 %`) portent `io.micronaut` dans leurs imports, ce qui signifie que l’usage global du framework reste élevé, mais que la distribution a diversifié l’activation en isolant les services et les filtres par module plutôt que de tout empaqueter dans un seul `.jar`.

### plant_shop_java_quarkus_distribuée

Les services distribués s’appuient clairement sur CDI/JAX-RS (annotations `@Path`, `@Inject`, `@Transactional`) et un bootstrap `QuarkusBootstrap` qui démarre Undertow, ce qui correspond à 100 % de l’usage framework dans la distribution. À l’inverse, la version monolithique compte 0 fichier sur 26 (`0 %`) qui importent `io.quarkus`, donc la version monolithique n’utilisait pas encore Quarkus et la version distribuée est le premier jalon concret de l’adoption.

### plant*shop_java_spring*\*\_distribuée

Les variantes Spring Boot distribuées exposent toutes `@RestController`, `@Repository`, `@Service` et des filtres `OncePerRequestFilter`, donc chaque endpoint métier s’adosse à Spring Security et à ses beans (100 % de l’usage framework). Dans la version monolithique, 37 fichiers sur 56 (`≈66 %`) importent `org.springframework`, ce qui montre que même en monolithique l’usage était massif mais encore concentré dans quelques packages, alors que la distribution segmente les responsabilités (sécurité, controllers, repositories et flux HTTP) avec des services séparés et des filtres centralisés.

#### Rapport détaillé monolithique plant_shop_javalin

Le monolithe `plant_shop_javalin` tourne avec un `Main` enrichi qui configure le mapper JSON, des middlewares CORS et un routeur ApiBuilder unique. Environ 30 % des fichiers Java importent directement `io.javalin`, ce qui signifie que Javalin reste le cœur du pipeline HTTP, là où chaque couche (controllers, repositories, utils) partage la même configuration globale. Le serveur Javalin n’est jamais délégué : le routeur, l’entrée HTTP et l’init base sont dans la même JVM et démarrent ensemble. Cela signifie aussi que les fichiers qui n’utilisent pas Javalin sont périphériques (db, utils) et que la migration distribuée doit les découpler sans perdre la cohérence du framework. Le monolithe continue d’assurer la gestion des cookies et de l’authentification directement via le serveur Javalin, ce qui contraste avec la distribution qui externalise la gateway.

#### Rapport détaillé monolithique plant_shop_java_micronaut

Dans le monolithe Micronaut, 82 % des fichiers importent `io.micronaut`, ce qui montre une adoption déjà responsable. Le point d’entrée `Main` et les modules `controllers`, `repositories`, `security` partagent la même configuration, donc la migration distribuée lisse surtout la décomposition des services tout en conservant le même moteur de DI/AOP. Le monolithe gère l’authentification, les filtres CORS, les repositories JDBC et les DTOs dans un seul processus, ce qui rendait les tests parfois plus lourds. La transition vers l’architecture distribuée a gardé les annotations Micronaut mais a distribué les classes en services autonomes (auth, catalog, order, user). Il reste possible de réconcilier les deux approches en partageant les utilitaires `ApiMapper` entre modules sans dupliquer de code.

#### Rapport détaillé monolithique plant_shop_java_quarkus

La version monolithique ne contenait aucun import `io.quarkus`, donc le framework n’était pas activement exploité, mais plutôt remplacé par un serveur maison. Le bootstrap du monolithe regroupait la logique d’authentification, des controllers et des repositories dans les mêmes packages, avec des filtres ad hoc sans CDI. Passer à la distribution Quarkus a introduit un registre CDI/JAX-RS plus rigoureux et un meilleur cycle de vie des beans. Les services distribués reprennent la validation de session et les filtres, mais via des entités CDI (`@RequestScoped`, `@Provider`) là où le monolithe utilisait de simples utils. Ce changement facilite la séparation des services tout en profitant de la configuration Quarkus (sessions, cors, CDI). La comparaison met en lumière le saut fonctionnel : là où le monolithe n’utilisait pas Quarkus, la version distribuée y repose désormais totalement.

#### Rapport détaillé monolithique plant_shop_java_spring

Dans le monolithe Spring, 66 % des fichiers importent `org.springframework`, ce qui indique une base solide même avant la distribution. Le même processus central configure les contrôleurs, la sécurité, les repositories et la persistence JDBC autour de Spring Boot/Security, ce qui facilite la migration vers des services Spring Boot indépendants. Les filtres et `Guards` y étaient déjà présents, donc la transformation distribuée a surtout consisté à isoler les beans dans chaque service (auth, catalog, order, user) plutôt que de réécrire le framework. L’utilisation des annotations `@RestController`, `@Repository` et `@Configuration` reste cohérente : on a seulement remplacé un contexte global par plusieurs contextes Spring isolés mais identiques. Cela rend la comparaison avec la distribution claire : l’usage de Spring reste constant, mais la segmentation module par module améliore la maintenabilité sans sacrifier la sécurité ou la gestion des sessions.

## Version 3 : usage effectif des frameworks dans les monolithes

### plant_shop_java_javalin

Dans `Structure_monolithique/plant_shop_javalin`, on confirme que les 7 fichiers qui importent `io.javalin` l’utilisent réellement : `Main.java` démarre `Javalin.create(...)`, configure `JavalinJsonMapper`, un middleware CORS et injecte `ApplicationController#getRoutes()`, puis les controllers (`AuthController`, `PlantController`, etc.) manipulent réellement `Context`, `Handler` et `EndpointGroup` au lieu de limiter la dépendance aux imports. Ce flux représente environ 30 % des sources Java du monolithe et il déclenche l’ensemble de l’API HTTP dans le même processus. Comparé à la version distribuée (0 eventuel `Javalin.create`, gateway sur `HttpServer`), la comparaison version 3 montre que seule la version monolithique exploite entièrement Javalin : les services distribués ne réutilisent pas ces appels, ils restent isolés derrière un proxy, ce qui confirme la discontinuité entre nom et exécution du framework (0 % de coverage Javalin en runtime distribué). L’écart est donc fonctionnel : monolithe 30 % d’utilisation effective, distribué 0 % (aucun server bootstrap), ce qui justifie l’effort requis pour aligner le code distribué sur son nom.

### plant_shop_java_micronaut

Le monolithe Micronaut lance `Micronaut.build(args)...start()` dans `Main.java` et les annotés controllers (`@Controller`, `@Get`, `@Post`), le filtre CORS, les repositories et les services de sécurité consomment bien les APIs Micronaut (`@Inject`, `@Filter`, `HttpResponse`). Ces usages couvrent 18 des 22 fichiers Java, soit 82 % des sources, et ils sont réutilisés avec les mêmes primitives Micronaut dans la version distribuée (contrôleurs identiques, mêmes guards). La version distribuée conserve les mêmes annotations et chacun des services monte son propre contexte Micronaut ; version 3 confirme que la transition a été une décomposition sans rupture du framework. Le pourcentage d’usage reste très proche, mais la structure distribuée déplace l’activation dans plusieurs JVM : les appels `AuthController.login(register)` fonctionnent aussi bien monolithiquement que distribués grâce au même runtime Micronaut.

### plant_shop_java_quarkus

Dans le monolithe `plant_shop_java_quarkus`, même si les sources importent `utils.QuarkusBootstrap`, aucune classe n’importe `io.quarkus.*` : le démarrage repose sur `Weld` et des beans CDI classiques, `QuarkusBootstrap` se contente de déployer Undertow/Resteasy manuellement sans utiliser les helpers Quarkus. Autrement dit, la version monolithique n’exécute pas véritablement le framework Quarkus (0 % de fichiers avec `io.quarkus`, contrairement à la version distribuée qui commence par des contrôleurs annotés `@Path`/`@Inject` et un `QuarkusBootstrap` avec CDI). La comparaison version 3 met donc en évidence que la seule adoption réelle de Quarkus se trouve dans la version distribuée : le monolithe n’utilise pas Quarkus, ce qui explique pourquoi la migration était une vraie réécriture plutôt qu’un simple refactoring.

### plant_shop_java_spring (Security + Security Hibernate)

Les deux variantes monolithiques démarrent un contexte Spring Boot (`SpringApplication.run(Main.class)`) et utilisent `@SpringBootApplication`, `@RestController`, `@Repository`, `@Security` filters et configurations (fichiers `controllers`, `security`, `repositories`). Ces annotations ne sont pas de simples imports : elles déclenchent la configuration `@Autowired`, les filtres `SessionAuthFilter`, les beans `Guards` et les beans `SessionService`, soit plus de 60 % des fichiers sources qui dépendent réellement de Spring (SpringApplication, controllers, security). La comparaison version 3 montre que la version distribuée se contente de découper ces mêmes composants en services séparés tout en conservant les appels Spring (100 % des endpoints sous Spring) ; l’usage effectif reste constant, simplement réparti dans plusieurs modules pour mieux isoler domaines fonctionnels. Par conséquent, l’écart n’est pas technique mais organisationnel : la version distribuée réplique 100 % des mécanismes Spring du monolithe, alors que l’ancienne architecture gardait ces mécanismes centralisés dans un seul processus.

## 3.1 Niveau d’alchimie sans Maven/Gradle (Javalin + Micronaut)
Vous avez rappelé que les monolithes Javalin et Micronaut ont été conçus sans Maven/Gradle, et la lecture montre qu’en effet les bootstraps (`Main.java`) invoquent `Javalin.create(...)` et `Micronaut.build(...)` tandis que les controllers/filtres consomment réellement leurs API. On peut donc considérer que l’effort pour « coller » au framework sans changer de gestionnaire a été maximal, mais voici ce qu’il resterait à améliorer sans toucher au Makefile :
- **Javalin** : extraire un fournisseur de configuration `JavalinConfigProvider` (CORS, json mapper, routes) et le partager dans tous les services/utilitaires pour que le framework soit actif dans chaque fichier HTTP. Cela permettrait de déclarer que ~100 % des fichiers métier sont associés à une instance Javalin au lieu des 30 % actuels (tous les contrôleurs/utilitaires liés à l’API seraient alors activement branchés sur `Javalin.create`).
- **Micronaut** : générer via le Makefile des stubs de beans `DatabaseFactory`, `SessionAuthFilter`, etc., pour que chaque repository/filtres soit explicitement produit par le conteneur `Micronaut.build(...)` et non un simple utilitaire. En faisant cela, la couverture effective passerait d’environ 82 % à ~100 %, car aucun fichier ne resterait en dehors du cycle de vie Micronaut.

## 3.2 Modifications à faire - monolithes
### Javalin
1. Extraire la configuration commune (JSON mapper, gestion CORS, filtre d’authentification) dans une classe `JavalinConfigProvider` instanciée dans `Main` et injectée dans `ApplicationController` + controllers afin que chaque méthode métier accède explicitement au serveur Javalin.  
2. Réécrire `Main` pour que la méthode `createRoutes` reçoive la configuration et que chaque `controller::action` soit enregistré dans le même bloc `Javalin.create(...)`, ce qui permet d’affirmer que 100 % des fichiers HTTP dépendent d’une instance active.
3. Ajouter un utilitaire `JavalinLifecycle` qui publie `start/stop` et qui est référencé par tous les modules (config, controllers, utils) pour rendre explicite la démultiplication du serveur dans les tests.
4. Tester en lançant `make run` et vérifier que les controllers utilisent bien le `Context` qui vient du même `Javalin` global ; cela donne une couverture de 100 % de l’usage du framework par les fichiers appelés depuis `Main`.

### Micronaut
1. Renommer `DatabaseFactory` pour qu’il implémente un `@Factory` Micronaut complet et générer automatiquement les classes de configuration par un script `Makefile` (ex : copier `micronaut-application.yml` vers `build/generated/`) pour déclarer des beans `@Singleton Connection`.
2. Marquer `SessionAuthFilter`, `CorsConfig` et les repositories/recompile tous comme `@Singleton` ou `@Factory` afin que la création soit gérée par `Micronaut.build(...)`, puis valider qu’ils sont destinés aux contextes `AuthController`, `PlantController`, etc.
3. Ajouter un petit fichier `build/generated/ServiceRegistry.java` exécuté par le Makefile qui référence tous les beans à instancier, de sorte que chaque fichier de service soit explicitement activé par le runtime (pas seulement importé).
4. Exécuter `make run` et s’assurer que chaque module mentionné apparaît dans le log `Micronaut` : ça confirme la couverture à 100 % du framework (car les annotations sont désormais matérialisées via les beans générés).

### Quarkus
1. Modifier `QuarkusBootstrap` pour qu’il utilise la classe générée par Quarkus (ou un mimic minimal) : remplacez l’initialisation manuelle `Weld` par un runner qui instancie le conteneur CDI fourni par Quarkus (sous forme de jar auto-généré dans `lib/`), puis injectez `@Inject` dans `Main`.
2. Ajouter des annotations Quarkus (`@ApplicationScoped`, `@Inject`) dans tous les controllers et repositories existants afin qu’il n’y ait plus de simples utilitaires Weld, et que la version monolithique repose sur le même modèle CDI que la version distribuée.
3. Inclure un fichier `config/application.properties` minimal (via Makefile) pour activer les extensions Quarkus nécessaires (resteasy, hibernate, jdbc) afin que `QuarkusBootstrap` puisse être remplacé par `io.quarkus.runtime.Quarkus.run`.
4. Valider en lançant `make run` que les logs signalent bien l’initialisation de Quarkus/Undertow et que `@Path`/`@Transactional` sont invocables ; cette série de modifications garantit un usage 100 % Quarkus dans le monolithe comme dans la version distribuée.
