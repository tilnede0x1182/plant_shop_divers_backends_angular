# Migrations SQL – Plant Shop Rust

Ce dossier contient les fichiers de migration SQL utilisés par SQLx.

- Placez chaque script de migration sous la forme : `YYYYMMDDHHMMSS_description.sql`
- Appliquez les migrations avec `sqlx migrate run` ou automatiquement via le serveur Rust.
- Exemple de première migration : `2025-init.sql` (structure initiale de la base)

**Convention** : chaque migration doit être idempotente et compatible PostgreSQL.

**Documentation SQLx** :
https://docs.rs/sqlx/latest/sqlx/migrate/index.html
