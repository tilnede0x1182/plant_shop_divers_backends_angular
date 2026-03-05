//! Requetes SeaORM pour les commandes.

// ==============================================================================
// Importations
// ==============================================================================

use std::collections::HashMap;

use chrono::{DateTime, Utc};
use sea_orm::prelude::DateTimeWithTimeZone;
use sea_orm::{DatabaseConnection, DbBackend, FromQueryResult, Statement};

use crate::errors::AppError;
use crate::orders::models::{OrderItemPlant, OrderItemResponse, OrderSummary};

// ==============================================================================
// Macros et Constantes
// ==============================================================================

macro_rules! order_base_sql {
    () => {
        r#"
SELECT
    owr.order_id          AS order_id,
    owr.status            AS order_status,
    owr.total             AS order_total,
    owr.created_at        AS order_created_at,
    owr.order_number      AS order_number,
    oi.id                 AS order_item_id,
    oi.quantity           AS order_item_quantity,
    oi.price              AS order_item_price,
    p.id                  AS plant_id,
    p.name                AS plant_name,
    p.price               AS plant_price
FROM orders_with_rank owr
LEFT JOIN order_items oi ON oi.order_id = owr.order_id
LEFT JOIN plants p ON p.id = oi.plant_id
"#
    };
}

const ROWS_FOR_USER_SQL: &str = concat!(
    order_base_sql!(),
    " WHERE owr.user_id = $1 ORDER BY owr.created_at DESC, oi.id ASC"
);

const ROWS_FOR_ORDER_SQL: &str = concat!(
    order_base_sql!(),
    " WHERE owr.order_id = $1 ORDER BY oi.id ASC"
);

// ==============================================================================
// Structures
// ==============================================================================

#[derive(Debug, FromQueryResult)]
/// Ligne brute de la requete SQL (avant agregation).
struct OrderFoldRow {
    order_id: i32,
    order_status: String,
    order_total: i32,
    order_created_at: DateTimeWithTimeZone,
    order_number: i64,
    order_item_id: Option<i32>,
    order_item_quantity: Option<i32>,
    order_item_price: Option<i32>,
    plant_id: Option<i32>,
    plant_name: Option<String>,
    plant_price: Option<i32>,
}

// ==============================================================================
// Fonctions
// ==============================================================================

/// Recupere les commandes d'un utilisateur avec leurs items.
///
/// @param db Connection a la base de donnees
/// @param user_id ID de l'utilisateur
/// @return Vec<OrderSummary> ou erreur
pub async fn summaries_for_user(
    db: &DatabaseConnection,
    user_id: i32,
) -> Result<Vec<OrderSummary>, AppError> {
    let rows = query_rows(db, ROWS_FOR_USER_SQL, user_id).await?;
    Ok(fold_rows(rows))
}

/// Recupere une commande par son ID avec ses items.
///
/// @param db Connection a la base de donnees
/// @param order_id ID de la commande
/// @return OrderSummary ou erreur 404
pub async fn summary_by_id(
    db: &DatabaseConnection,
    order_id: i32,
) -> Result<OrderSummary, AppError> {
    let rows = query_rows(db, ROWS_FOR_ORDER_SQL, order_id).await?;
    fold_rows(rows).into_iter().next().ok_or(AppError::NotFound)
}

/// Execute une requete SQL et retourne les lignes brutes.
///
/// @param db Connection a la base de donnees
/// @param sql Requete SQL parametree
/// @param bind Valeur du parametre $1
/// @return Vec<OrderFoldRow> ou erreur
async fn query_rows(
    db: &DatabaseConnection,
    sql: &str,
    bind: i32,
) -> Result<Vec<OrderFoldRow>, AppError> {
    let stmt = Statement::from_sql_and_values(DbBackend::Postgres, sql, vec![bind.into()]);
    OrderFoldRow::find_by_statement(stmt)
        .all(db)
        .await
        .map_err(|_| AppError::Internal)
}

/// Agrege les lignes SQL en OrderSummary (fold/reduce).
///
/// @param rows Lignes brutes de la requete
/// @return Vec<OrderSummary> agreges
fn fold_rows(rows: Vec<OrderFoldRow>) -> Vec<OrderSummary> {
    let mut summaries = Vec::new();
    let mut index = HashMap::new();

    for row in rows {
        let entry = index.entry(row.order_id).or_insert_with(|| {
            let summary = OrderSummary::new(
                row.order_id,
                row.order_status.clone(),
                row.order_total,
                to_utc(row.order_created_at),
                Some(row.order_number),
                Vec::new(),
            );
            summaries.push(summary);
            summaries.len() - 1
        });

        if let Some(item) = map_row_to_item(&row) {
            summaries[*entry].items.push(item);
        }
    }

    summaries
}

/// Convertit une ligne SQL en OrderItemResponse.
///
/// @param row Reference a une ligne brute
/// @return Some(OrderItemResponse) ou None si item incomplet
fn map_row_to_item(row: &OrderFoldRow) -> Option<OrderItemResponse> {
    let item_id = row.order_item_id?;
    let plant_id = row.plant_id?;
    let item_price = row.order_item_price?;
    let plant_price = row.plant_price?;

    Some(OrderItemResponse::new(
        item_id,
        plant_id,
        row.order_item_quantity.unwrap_or(0),
        item_price,
        OrderItemPlant {
            id: plant_id,
            name: row.plant_name.clone().unwrap_or_default(),
            price: plant_price,
        },
    ))
}

/// Convertit un DateTimeWithTimeZone en DateTime<Utc>.
///
/// @param value Valeur avec timezone
/// @return DateTime<Utc>
fn to_utc(value: DateTimeWithTimeZone) -> DateTime<Utc> {
    DateTime::<Utc>::from(value)
}
