//! Module de cache en memoire.

// ==============================================================================
// Importations
// ==============================================================================

use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;

use crate::dto::{PlantResponse, UserResponse};

// ==============================================================================
// Traits
// ==============================================================================

/// Trait pour le cache utilisateurs.
pub trait UserCache: Send + Sync {
    fn get(&self) -> impl std::future::Future<Output = Option<Vec<UserResponse>>> + Send;
    fn set(&self, data: &[UserResponse]) -> impl std::future::Future<Output = ()> + Send;
    fn invalidate(&self) -> impl std::future::Future<Output = ()> + Send;
}

/// Trait pour le cache plantes.
pub trait PlantCache: Send + Sync {
    fn get(&self) -> impl std::future::Future<Output = Option<Vec<PlantResponse>>> + Send;
    fn set(&self, data: &[PlantResponse]) -> impl std::future::Future<Output = ()> + Send;
    fn invalidate(&self) -> impl std::future::Future<Output = ()> + Send;
}

/// Cache partage pour les utilisateurs.
pub type SharedUserCache = Arc<dyn UserCache>;
/// Cache partage pour les plantes.
pub type SharedPlantCache = Arc<dyn PlantCache>;

// ==============================================================================
// Structures
// ==============================================================================

/// Cache avec expiration temporelle pour une liste d'elements.
struct TimedVecCache<T> {
    data: RwLock<Option<(Vec<T>, Instant)>>,
    ttl: Duration,
}

impl<T: Clone + Send + Sync> TimedVecCache<T> {
    fn new(ttl: Duration) -> Self {
        Self {
            data: RwLock::new(None),
            ttl,
        }
    }

    async fn get_data(&self) -> Option<Vec<T>> {
        let guard = self.data.read().await;
        if let Some((ref vec, instant)) = *guard {
            if instant.elapsed() < self.ttl {
                return Some(vec.clone());
            }
        }
        None
    }

    async fn set_data(&self, items: &[T]) {
        let mut guard = self.data.write().await;
        *guard = Some((items.to_vec(), Instant::now()));
    }

    async fn clear(&self) {
        let mut guard = self.data.write().await;
        *guard = None;
    }
}

// ==============================================================================
// Implementations
// ==============================================================================

impl UserCache for TimedVecCache<UserResponse> {
    /// Implementation du cache utilisateurs.
    async fn get(&self) -> Option<Vec<UserResponse>> { self.get_data().await }
    async fn set(&self, data: &[UserResponse]) { self.set_data(data).await }
    async fn invalidate(&self) { self.clear().await }
}

impl PlantCache for TimedVecCache<PlantResponse> {
    /// Implementation du cache plantes.
    async fn get(&self) -> Option<Vec<PlantResponse>> { self.get_data().await }
    async fn set(&self, data: &[PlantResponse]) { self.set_data(data).await }
    async fn invalidate(&self) { self.clear().await }
}

// ==============================================================================
// Fonctions
// ==============================================================================

/// Cree un cache utilisateurs avec le TTL specifie.
pub fn default_user_cache(ttl: Duration) -> SharedUserCache {
    Arc::new(TimedVecCache::<UserResponse>::new(ttl))
}

/// Cree un cache plantes avec le TTL specifie.
pub fn default_plant_cache(ttl: Duration) -> SharedPlantCache {
    Arc::new(TimedVecCache::<PlantResponse>::new(ttl))
}
