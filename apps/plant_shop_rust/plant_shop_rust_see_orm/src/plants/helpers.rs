use crate::entity::plants::ActiveModel as ActivePlant;
use crate::plants::models::UpdatePlant;
use sea_orm::Set;

pub fn apply_plant_updates(active: &mut ActivePlant, payload: &UpdatePlant) {
    if let Some(name) = payload.name.clone() {
        active.name = Set(name);
    }
    if let Some(desc) = payload.description.clone() {
        active.description = Set(Some(desc));
    }
    if let Some(price) = payload.price {
        active.price = Set(price);
    }
    if let Some(stock) = payload.stock {
        active.stock = Set(stock);
    }
}
