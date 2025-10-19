use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Clone)]
pub struct OrderItem {
    pub id: i32,
		#[allow(dead_code)]
    #[serde(skip_serializing)]
    pub order_id: Option<i32>,
    pub plant_id: Option<i32>,
    pub quantity: i32,
		pub price: i32,
}

#[derive(Deserialize)]
pub struct NewOrderItem {
    pub order_id: i32,
    pub plant_id: i32,
    pub quantity: i32,
    pub price: i32,
}
