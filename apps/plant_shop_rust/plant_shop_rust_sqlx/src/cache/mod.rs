use std::sync::Arc;

use async_trait::async_trait;
use tokio::sync::RwLock;

use crate::dto::{PlantResponse, UserResponse};

#[async_trait]
pub trait UserCache: Send + Sync {
    async fn get(&self) -> Option<Vec<UserResponse>>;
    async fn set(&self, data: &[UserResponse]);
    async fn invalidate(&self);
}

#[async_trait]
pub trait PlantCache: Send + Sync {
    async fn get(&self) -> Option<Vec<PlantResponse>>;
    async fn set(&self, data: &[PlantResponse]);
    async fn invalidate(&self);
}

#[derive(Default)]
pub struct InMemoryUserCache {
    inner: RwLock<Option<Vec<UserResponse>>>,
}

#[derive(Default)]
pub struct InMemoryPlantCache {
    inner: RwLock<Option<Vec<PlantResponse>>>,
}

#[async_trait]
impl UserCache for InMemoryUserCache {
    async fn get(&self) -> Option<Vec<UserResponse>> {
        self.inner.read().await.clone()
    }

    async fn set(&self, data: &[UserResponse]) {
        self.inner.write().await.replace(data.to_vec());
    }

    async fn invalidate(&self) {
        self.inner.write().await.take();
    }
}

#[async_trait]
impl PlantCache for InMemoryPlantCache {
    async fn get(&self) -> Option<Vec<PlantResponse>> {
        self.inner.read().await.clone()
    }

    async fn set(&self, data: &[PlantResponse]) {
        self.inner.write().await.replace(data.to_vec());
    }

    async fn invalidate(&self) {
        self.inner.write().await.take();
    }
}

pub type SharedUserCache = Arc<dyn UserCache>;
pub type SharedPlantCache = Arc<dyn PlantCache>;

pub fn default_user_cache() -> SharedUserCache {
    Arc::new(InMemoryUserCache::default())
}

pub fn default_plant_cache() -> SharedPlantCache {
    Arc::new(InMemoryPlantCache::default())
}
