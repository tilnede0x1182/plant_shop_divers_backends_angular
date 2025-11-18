# RAPPORT D'ANALYSE DES DUPLICATIONS DE CODE (Principe DRY)

## Introduction
Backend Flask + psycopg2 situé dans `plant_shop_python`, structuré autour de contrôleurs (`controllers/*.py`) et de repositories (`repositories/*.py`).

---

## Violations DRY

### 1. Sérialisation `__dict__` répétée dans les contrôleurs - 🟠 Haute
`controllers/plants.py:13-57` et `controllers/users.py` (pattern identique) font toujours `return json_response([p.__dict__ for p in repo.list()])`. Chaque contrôleur reconstruit la même boucle pour créer un payload. **Action** : offrir des DTO (`Plant.to_dict()`, `User.to_public_dict()`) ou un helper `json_list(repo.list(), mapper)` afin de ne plus dupliquer la logique.

### 2. `_map_from_row` cloné dans tous les repositories - 🔴 Critique
`repositories/plants.py:10-19`, `repositories/users.py:10-21`, `repositories/orders.py:10-32` et `repositories/order_items.py` possèdent tous la même boucle `col_map = {}` + instanciation du modèle. Toute modification de mapping (nouvelle colonne) doit être réécrite. **Action** : remonter cette logique dans `BaseRepository` via un utilitaire `map_row(row, columns, dataclass)` ou utiliser `NamedTuple`/`dataclass` + `cursor_factory=RealDictCursor`.

### 3. Sérialisation d’une commande dupliquée - 🟠 Haute
`controllers/orders.py:11-38` définit `_serialize_item` et `_serialize_order`, alors que `repositories/orders.py:10-74` dispose déjà de tous les champs nécessaires (et `repositories/order_items.py` reconstruit encore ces objets). D’autres couches (tests) devront refaire la même chose. **Action** : déplacer `_serialize_order` dans `models/order.py` (ex. méthode `to_dict(include_items=True)`) et l’utiliser partout.

---

## Impact estimé

| Refactoring proposé                            | Lignes supprimées | Modules touchés                    |
|------------------------------------------------|-------------------|------------------------------------|
| DTO / `to_dict()` sur les modèles              | ~60               | controllers/plants & users         |
| Mapper générique dans `BaseRepository`         | ~90               | tous les repositories              |
| Sérialisation Order centralisée                | ~70               | controllers/orders, repositories   |

---

## Conclusion
Le backend Flask recopie les mêmes conversions (row → modèle → dict) dans chaque couche. Centraliser ces étapes (DTO/mapper) est indispensable pour maintenir la cohérence des réponses et appliquer le principe DRY.***
