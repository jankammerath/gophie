//! Configuration, theming, and system utilities for Gophie.

mod config_file;
mod config_manager;
mod system_util;
mod theme;

pub use config_file::ConfigFile;
pub use config_manager::ConfigManager;
pub use system_util::SystemUtility;
pub use theme::{parse_hex_color, Theme};
