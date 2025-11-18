use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

use crate::entity::users::Model as UserModel;

#[derive(Serialize, Deserialize, Clone)]
pub struct User {
    pub id: i32,
    pub email: String,
    #[serde(rename = "name")]
    pub username: String,
    #[serde(rename = "admin")]
    pub is_admin: bool,
    pub created_at: DateTime<Utc>,
}

impl From<UserModel> for User {
    fn from(model: UserModel) -> Self {
        Self {
            id: model.id,
            email: model.email,
            username: model.username,
            is_admin: model.is_admin,
            created_at: model.created_at.into(),
        }
    }
}

impl From<&UserModel> for User {
    fn from(model: &UserModel) -> Self {
        Self {
            id: model.id,
            email: model.email.clone(),
            username: model.username.clone(),
            is_admin: model.is_admin,
            created_at: model.created_at.into(),
        }
    }
}

#[derive(Deserialize, Serialize, Debug)]
pub struct UpdateUser {
    #[serde(alias = "name")]
    pub name: Option<String>,
    pub email: Option<String>,
    #[serde(alias = "admin")]
    pub admin: Option<bool>,
}

#[allow(dead_code)]
#[derive(Deserialize)]
pub struct NewUser {
    pub name: String,
    pub email: String,
    pub password: String,
}
