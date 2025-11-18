# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Frontend Angular Universal (standalone components) couvrant les espaces public, utilisateur et admin.

---

## Violations DRY

### 1. Service API monolithique et répétitif - 🔴 Critique
`src/app/services/api.service.ts:33-100` expose 17 méthodes quasi identiques (`listerPlantes`, `listerPlantesAdmin`, `listerUtilisateurs`, `listerUtilisateursAdmin`, etc.) qui ne différant que par l’URL de base (`/plants` vs `/admin/plants`). Toute modification d’en-tête ou de gestion d’erreur doit être multipliée. **Action** : découper par ressource (`PlantsApi`, `UsersApi`, `OrdersApi`) ou introduire un helper `request<T>(method, path, body?)` avec des builders (`resource('plants').getAll()`). On respecte DRY et on prépare la gestion centralisée des erreurs.

### 2. Composants d’édition utilisateur dupliqués - 🟠 Haute
`AdminUserEditComponent` (`src/app/admin/users/user-profile-edit/user-profile-edit.component.ts:1-52`) et `UserProfileEditComponent` (`src/app/users/user-profile-edit/user-profile-edit.component.ts:1-51`) partagent la totalité du code (imports, injection `ApiService`, récupération de l’ID via `ActivatedRoute`) à l’exception de la méthode API appelée et de la redirection. **Action** : factoriser un composant `UserEditorComponent` paramétré (`@Input() mode: 'self' | 'admin'`) ou un hook `useUserEditor` réutilisé par les deux vues pour supprimer cette duplication.

### 3. Déclaration des gardes Admin répétée dans les routes - 🟠 Haute
`src/app/app.routes.ts:8-86` répète `canActivate: [AdminGuard]` pour chaque chemin `/admin/...` (8 occurrences) alors que tous partagent la même contrainte. **Action** : créer un routeur enfant `path: 'admin', canActivateChild: [AdminGuard], loadChildren: ...` ou utiliser `canMatch` global. Cela supprime la répétition et garantit que l’ajout d’une route admin ne nécessite pas d’oublier la garde.

---

## Impact estimé

| Refactoring proposé                          | Lignes supprimées | Zones concernées                        |
|----------------------------------------------|-------------------|----------------------------------------|
| Helper + services spécialisés pour l’API     | ~120              | `src/app/services/api.service.ts`      |
| Composant partagé pour l’édition utilisateur | ~70               | `admin/users/...`, `users/...`         |
| Regroupement des routes admin                | ~40               | `src/app/app.routes.ts`                |

---

## Conclusion
Le front Angular multiplie les points d’entrée identiques (services, composants, routes) ce qui complexifie toute évolution (nouveaux headers, changement d’URL). Mutualiser ces éléments est essentiel pour rester aligné avec le principe DRY du projet.***
