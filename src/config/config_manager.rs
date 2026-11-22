//! Application configuration manager and standard directory resolution.

use std::fs;
use std::path::{Path, PathBuf};

use super::{ConfigFile, Theme};

/// Default Gopher homepage loaded on startup when `GOPHERHOME` is not configured.
pub const DEFAULT_GOPHERHOME: &str = "gopher.floodgap.com";
/// Default text encoding used to decode Gopher menus and text files.
pub const DEFAULT_CHARSET: &str = "UTF-8";

const CONFIG_DIR_NAME: &str = "Gophie";
const CONFIG_FILE_NAME: &str = "config.ini";

/// Manages Gophie's persistent configuration file, theme, and user directories.
#[derive(Debug, Clone)]
pub struct ConfigManager {
    config_file: ConfigFile,
    theme: Theme,
    download_dir: PathBuf,
}

impl ConfigManager {
    /// Initializes the configuration manager using the user's home directory (`~/Gophie/config.ini`).
    pub fn load_default() -> Self {
        let config_dir = Self::resolve_config_dir();
        let _ = fs::create_dir_all(&config_dir);

        let config_path = config_dir.join(CONFIG_FILE_NAME);
        let config_file = ConfigFile::load(config_path);
        let theme = Theme::from_config(&config_file);
        let download_dir = Self::resolve_download_dir();

        Self {
            config_file,
            theme,
            download_dir,
        }
    }

    /// Returns a reference to the underlying INI configuration file.
    pub fn config_file(&self) -> &ConfigFile {
        &self.config_file
    }

    /// Returns the resolved UI theme derived from `config.ini`.
    pub fn theme(&self) -> &Theme {
        &self.theme
    }

    /// Returns the configured Gopher homepage URL.
    pub fn gopher_home(&self) -> String {
        self.config_file
            .get_setting("GOPHERHOME", "Navigation", DEFAULT_GOPHERHOME)
            .to_string()
    }

    /// Updates `GOPHERHOME` in `config.ini` and persists it to disk.
    pub fn set_gopher_home(&mut self, url: &str) {
        self.config_file.set_setting("GOPHERHOME", url, "Navigation");
        if let Err(err) = self.config_file.save() {
            eprintln!("Failed to save config file: {}", err);
        }
    }

    /// Returns whether RFC 4266 item-type prefixes (e.g. `/1/...`) are shown in the address bar.
    pub fn selector_prefix_enabled(&self) -> bool {
        self.config_file
            .get_bool("SELECTOR_PREFIX_ENABLED", "Navigation", true)
    }

    /// Returns the configured default character set name (e.g. `UTF-8`, `ISO-8859-1`).
    pub fn default_charset(&self) -> String {
        self.config_file
            .get_setting("DEFAULT_CHARSET", "Network", DEFAULT_CHARSET)
            .to_string()
    }

    /// Returns the default directory for storing downloaded files.
    pub fn download_dir(&self) -> &Path {
        &self.download_dir
    }

    /// Resolves `~/Gophie/` across macOS, Windows, and Linux.
    fn resolve_config_dir() -> PathBuf {
        dirs::home_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join(CONFIG_DIR_NAME)
    }

    /// Resolves the user's Downloads directory across platforms.
    fn resolve_download_dir() -> PathBuf {
        dirs::download_dir().unwrap_or_else(|| {
            dirs::home_dir()
                .unwrap_or_else(|| PathBuf::from("."))
                .join("Downloads")
        })
    }
}
