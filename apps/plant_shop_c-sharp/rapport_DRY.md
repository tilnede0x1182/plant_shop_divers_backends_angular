# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Le backend C# regroupe trois implémentations : `plant_shop_c-sharp` (serveur HttpListener + Npgsql), `plant_shop_asp_dapper` et `plant_shop_asp_EF_core`. Le rapport couvre ces trois variantes.

---

## Violations DRY

### 1. Routage manuel dupliqué dans HttpListener - 🔴 Critique
Les contrôleurs `PlantController`, `OrderController` et `UserController` redéfinissent chacun un `HandleRequest` qui découpe l’URL, lit `segments` et dispatch en fonction de `method` (`plant_shop_c-sharp/Controllers/PlantController.cs:13-67`, `OrderController.cs:15-67`, `UserController.cs:14-95`). Le même code (split, `int.TryParse`, tests admin) occupe >150 lignes. **Action** : factoriser ce routage dans `BaseController` (pattern `RouteDescriptor{Path,Method,Guard,Handler}`) ou migrer vers ASP.NET minimal APIs pour bénéficier du router intégré.

### 2. Mapping ADO.NET répété dans chaque repository - 🟠 Haute
`PlantRepository` (`plant_shop_c-sharp/Repositories/PlantRepository.cs:12-92`), `OrderRepository` et `UserRepository` recréent tous la même séquence ADO.NET : `GetConnection()`, `new NpgsqlCommand`, binding de paramètres et `MapXxx`. Hormis le SQL, la structure est identique. **Action** : introduire des helpers génériques dans `BaseRepository` (`QuerySingle<T>(sql, params, mapper)`, `Execute(sql, params)`) pour supprimer ces doublons et centraliser la gestion des connexions/transactions.

### 3. Triplication fonctionnelle entre HttpListener, Dapper et EF Core - 🟠 Haute
Les contrôleurs `plant_shop_asp_dapper/Controllers/PlantController.cs:19-67` et `plant_shop_asp_EF_core/Controllers/PlantsController.cs:19-118` ré-implémentent les mêmes endpoints CRUD avec les mêmes validations que la version HttpListener (`plant_shop_c-sharp/Controllers/PlantController.cs:69-147`). On maintient trois piles distinctes pour le même métier. **Action** : extraire la logique métier (DTO, validation, calculs) dans une bibliothèque partagée (ex. `PlantShop.Application`) et ne laisser que l’adaptation infrastructurelle (Dapper vs EF vs HttpListener) dans chaque projet.

---

## Impact estimé

| Refactoring proposé                                   | Lignes supprimées | Périmètre               | Complexité |
|-------------------------------------------------------|-------------------|-------------------------|------------|
| Router commun / mini framework HttpListener           | ~150              | Controllers HttpListener| Moyenne    |
| Helpers génériques pour accès Npgsql                  | ~120              | Repositories HttpListener| Basse      |
| Extraction du domaine commun (services + DTO)         | 200+ (3 projets)  | HttpListener/Dapper/EF  | Haute      |

---

## Conclusion
Sans mutualisation, chaque correction (nouvelle route, changement de DTO) doit être appliquée trois fois. Priorité : partager la logique métier (services + DTO) et éliminer le routage artisanal pour respecter le principe DRY imposé au monorepo.***
