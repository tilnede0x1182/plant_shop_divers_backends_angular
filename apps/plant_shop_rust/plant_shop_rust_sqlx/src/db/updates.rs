use sqlx::{Postgres, QueryBuilder};

/// Helper pour construire des requêtes UPDATE avec `COALESCE`.
pub struct PartialUpdate {
    builder: QueryBuilder<'static, Postgres>,
    has_set: bool,
}

impl PartialUpdate {
    pub fn new(table: &'static str) -> Self {
        let mut builder = QueryBuilder::new("UPDATE ");
        builder.push(table);
        Self {
            builder,
            has_set: false,
        }
    }

    pub fn set_with_coalesce<V>(&mut self, column: &str, value: V)
    where
        V: sqlx::Encode<'static, Postgres> + sqlx::Type<Postgres> + Send + 'static,
    {
        if !self.has_set {
            self.builder.push(" SET ");
        } else {
            self.builder.push(", ");
        }
        self.builder.push(column);
        self.builder.push(" = COALESCE(");
        self.builder.push_bind(value);
        self.builder.push(", ");
        self.builder.push(column);
        self.builder.push(")");
        self.has_set = true;
    }

    pub fn finish(self) -> QueryBuilder<'static, Postgres> {
        self.builder
    }
}
