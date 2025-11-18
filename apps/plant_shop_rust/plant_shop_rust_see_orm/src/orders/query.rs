use std::collections::HashMap;

use chrono::{DateTime, Utc};
use sea_orm::prelude::DateTimeWithTimeZone;
use sea_orm::{DatabaseConnection, DbBackend, FromQueryResult, Statement};

use crate::errors::AppError;
use crate::orders::models::{OrderItemPlant, OrderItemResponse, OrderSummary};

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

#[derive(Debug, FromQueryResult)]
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

pub async fn summaries_for_user(
    db: &DatabaseConnection,
    user_id: i32,
) -> Result<Vec<OrderSummary>, AppError> {
    let rows = query_rows(db, ROWS_FOR_USER_SQL, user_id).await?;
    Ok(fold_rows(rows))
}

pub async fn summary_by_id(
    db: &DatabaseConnection,
    order_id: i32,
) -> Result<OrderSummary, AppError> {
    let rows = query_rows(db, ROWS_FOR_ORDER_SQL, order_id).await?;
    fold_rows(rows).into_iter().next().ok_or(AppError::NotFound)
}

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

fn to_utc(value: DateTimeWithTimeZone) -> DateTime<Utc> {
    DateTime::<Utc>::from(value)
}
