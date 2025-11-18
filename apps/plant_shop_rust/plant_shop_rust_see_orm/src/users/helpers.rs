use crate::entity::users::ActiveModel as ActiveUser;
use crate::users::models::UpdateUser;
use sea_orm::Set;

pub fn apply_user_updates(active: &mut ActiveUser, payload: &UpdateUser, can_toggle_admin: bool) {
    if let Some(name) = payload.name.clone() {
        active.username = Set(name);
    }
    if let Some(email) = payload.email.clone() {
        active.email = Set(email);
    }
    if can_toggle_admin {
        if let Some(admin) = payload.admin {
            active.is_admin = Set(admin);
        }
    }
}
