# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Audit du backend Go (`plant_shop_go`) structuré en handlers Gorilla/Mux et services Gorm.

---

## Violations DRY

### 1. Arrondi des prix répété dans tous les handlers - 🔴 Critique
`internal/http/handlers/plants.go:13-35` et `plants_admin.go:14-55` reconduisent la même boucle `plants[i].Price = float64(int(price*100))/100`. La même opération est recopiée dans `orders.go:61-71`, `orders.go:166-181` et même lors du calcul du total (lignes 121-180). **Action** : exposer `models.NormalizePrice(p *models.Plant)` ou un utilitaire `money.Round2` invoqué depuis un middleware de sérialisation pour éviter les divergences (certains endroits utilisent `math.Round`, d’autres `int`).

### 2. Extraction des claims JWT clonée - 🟠 Haute
Les fonctions `ListUserOrders` (`orders.go:37-75`) et `CreateOrder` (`orders.go:89-182`) effectuent exactement les mêmes vérifications (`raw := r.Context().Value("claims")`, cast en `*security.Claims`, gestion des erreurs HTTP). Chaque nouveau handler authentifié recrée cette séquence. **Action** : ajouter un middleware/helper `claims := middleware.MustClaims(r)` qui retourne directement l’ID utilisateur ou renvoie `401`, puis l’utiliser partout (orders, users, admin).

### 3. Parsing d’identifiants HTTP dispersé - 🟠 Haute
Le code lit les identifiants tantôt via `mux.Vars` (`plants_admin.go:58-125`, `orders.go:185-219`), tantôt via `strings.Split` (`plants.go:26-35`). Cette duplication multiplie les conversions `strconv.ParseUint` + messages d’erreur. **Action** : créer `helpers.ParseID(r *http.Request) (uint64, error)` et l’utiliser dans toutes les routes `/resource/{id}` afin d’avoir un seul message erreur + un seul chemin de validation.

---

## Impact estimé

| Refactoring proposé                               | Lignes supprimées | Fichiers touchés      | Complexité |
|---------------------------------------------------|-------------------|-----------------------|------------|
| Normalisation unique des prix                     | ~80               | plants*, orders.go    | Basse      |
| Helper/middleware pour récupérer les claims       | ~60               | orders, users, admin  | Basse      |
| Helper `ParseID` commun                            | ~70               | plants_admin, orders  | Basse      |

---

## Conclusion
Les handlers Go dupliquent beaucoup de plomberie (arrondis, parsing d’ID, récupération des claims). En isolant ces aspects dans des helpers/middlewares, on réduit l’entropie et on prépare des évolutions (changement du cookie ou des formats monétaires) sans casser les routes.***
