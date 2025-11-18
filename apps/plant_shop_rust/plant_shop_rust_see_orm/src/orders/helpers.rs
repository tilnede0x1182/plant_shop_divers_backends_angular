use crate::entity::{order_items, orders::Model as OrderModel, plants::Model as PlantModel};
use crate::orders::models::{OrderItemPlant, OrderItemResponse, OrderSummary};

pub fn build_order_item_response(item: order_items::Model, plant: PlantModel) -> OrderItemResponse {
    let plant_view = OrderItemPlant {
        id: plant.id,
        name: plant.name,
        price: plant.price,
    };

    OrderItemResponse::new(item.id, plant.id, item.quantity, item.price, plant_view)
}

pub fn build_order_summary(order: OrderModel, items: Vec<OrderItemResponse>) -> OrderSummary {
    OrderSummary::new(
        order.id,
        order.status,
        order.total,
        order.created_at.into(),
        items,
    )
}
