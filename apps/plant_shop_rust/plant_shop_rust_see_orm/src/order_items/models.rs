use serde::{Serialize, Deserialize};
use sea_orm::prelude::Decimal;

#[derive(Serialize, Deserialize, Clone)]
pub struct OrderItem {
    pub id: i32,
		#[allow(dead_code)]
    #[serde(skip_serializing)]
    pub order_id: Option<i32>,
    pub plant_id: Option<i32>,
    pub quantity: i32,
		#[serde(serialize_with = "crate::plants::models::serialize_Decimal_as_i32")]
		pub price: Decimal,
}

#[derive(Deserialize)]
pub struct NewOrderItem {
    pub order_id: i32,
    pub plant_id: i32,
    pub quantity: i32,
    pub price: Decimal,
}
