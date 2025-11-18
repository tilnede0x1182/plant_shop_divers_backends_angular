use std::sync::OnceLock;

use crate::errors::AppError;

static DEBUG_ENABLED: OnceLock<bool> = OnceLock::new();

fn is_enabled() -> bool {
    *DEBUG_ENABLED.get_or_init(|| match std::env::var("DEBUG") {
        Ok(value) => matches!(value.to_lowercase().as_str(), "1" | "true" | "on" | "yes"),
        Err(_) => false,
    })
}

#[allow(dead_code)]
pub fn log_debug(message: impl AsRef<str>) {
    if is_enabled() {
        eprintln!("[DEBUG] {}", message.as_ref());
    }
}

pub fn log_debug_lazy<F>(builder: F)
where
    F: FnOnce() -> String,
{
    if is_enabled() {
        eprintln!("[DEBUG] {}", builder());
    }
}

#[allow(dead_code)]
pub fn log_error(message: impl AsRef<str>, error: &AppError) {
    if is_enabled() {
        eprintln!("[DEBUG][ERROR] {} -> {}", message.as_ref(), error);
    }
}
