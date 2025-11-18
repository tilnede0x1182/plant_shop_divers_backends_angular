# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Backend NestJS (`apps/plant_shop_nest`) structuré en modules Auth/Plants/Orders/Users avec Prisma.

---

## Violations DRY

### 1. Méthodes alias en doublon dans les services - 🟠 Haute
`PlantsService` expose `list`, `findAll`, `one`, `findOne` qui appellent simplement la méthode voisine (`src/app/plants/plants.service.ts:14-46`). Ces duplications gonflent le service et ouvrent la porte aux divergences (un alias peut oublier un `await`). **Action** : conserver une seule méthode par opération (`findAll`, `findOne`) et, si besoin, ajouter des alias via décorateurs, pas de copies complètes.

### 2. Bloc Prisma `include` recopié quatre fois dans OrdersService - 🔴 Critique
Dans `OrdersService` (`src/app/orders/orders.service.ts:19-69`), chaque méthode (`list`, `findAll`, `one`, `findOneForUser`) répète `include: { orderItems: { include: { plant: true } } }`. Toute évolution (ajout d’un champ, renommage) demande 4 modifications. **Action** : extraire une constante `const orderWithItems = { include: { orderItems: { include: { plant: true } } } };` et l’étendre (`return this.prisma.order.findMany({ ...orderWithItems, where: { ... } })`).

### 3. Garde admin appliquée manuellement route par route - 🟠 Haute
`plants.controller.ts` répète trois fois `@UseGuards(JwtAuthGuard, RolesGuard)` + `@Roles('admin')` (lignes 34-51) et `users.controller.ts` cumule `@UseGuards` au niveau classe ET méthode (`src/app/users/users.controller.ts:20-80`). Cette duplication augmente le risque d’oublier la garde sur une nouvelle route admin. **Action** : créer un décorateur composite (`@AdminOnly()`) qui encapsule garde + rôle, ou un module `AdminModule` avec `@UseGuards` global.

---

## Impact estimé

| Refactoring proposé                          | Lignes supprimées | Modules touchés | Complexité |
|----------------------------------------------|-------------------|-----------------|------------|
| Supprimer les alias redondants (PlantsService)| ~20               | PlantsService   | Basse      |
| Mutualiser l’`include` Prisma                | ~40               | OrdersService   | Basse      |
| Décorateur/garde admin unique                | ~30               | Plants & Users  | Basse      |

---

## Conclusion
Les duplications concernent surtout la couche service/controller : alias inutiles, garde admin répétée, requêtes Prisma clonées. En les supprimant, on fiabilise les évolutions (ex. ajout d’un champ OrderItem) tout en respectant la priorité DRY donnée au projet.***
