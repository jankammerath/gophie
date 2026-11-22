//! Application configuration manager and standard directory resolution.

use std::fs;
use std::io;
use std::path::{Path, PathBuf};

use super::{ConfigFile, Theme};

/// Default Gopher homepage loaded on startup when `GOPHERHOME` is not configured.
pub const DEFAULT_GOPHERHOME: &str = "gopher.floodgap.com";
/// Default Type-7 Gopher search engine (Veronica-2 on Floodgap).
pub const DEFAULT_SEARCH_ENGINE: &str = "gopher.floodgap.com/v2/vs";
/// Default text encoding used to decode Gopher menus and text files.
pub const DEFAULT_CHARSET: &str = "UTF-8";

const CONFIG_DIR_NAME: &str = "Gophie";
const CONFIG_FILE_NAME: &str = "config.ini";
const DOWNLOADS_HISTORY_FILE_NAME: &str = "downloads.tsv";

/// Editable snapshot of all user-facing configuration values shown in the Settings dialog.
#[derive(Debug, Clone)]
pub struct AppSettings {
    pub gopher_home: String,
    pub search_engine: String,
    pub selector_prefix_enabled: bool,
    pub default_charset: String,
    pub theme: Theme,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            gopher_home: DEFAULT_GOPHERHOME.to_string(),
            search_engine: DEFAULT_SEARCH_ENGINE.to_string(),
            selector_prefix_enabled: true,
            default_charset: DEFAULT_CHARSET.to_string(),
            theme: Theme::default(),
        }
    }
}

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

    /// Returns the configured Type-7 Gopher search engine URL.
    pub fn search_engine(&self) -> String {
        self.config_file
            .get_setting("SEARCH_ENGINE", "Navigation", DEFAULT_SEARCH_ENGINE)
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

    /// Returns the path to the persistent download history file (`~/Gophie/downloads.tsv`).
    pub fn downloads_history_path(&self) -> PathBuf {
        self.config_file
            .path()
            .parent()
            .unwrap_or_else(|| Path::new("."))
            .join(DOWNLOADS_HISTORY_FILE_NAME)
    }

    /// Builds an editable snapshot of all current configuration settings.
    pub fn settings(&self) -> AppSettings {
        AppSettings {
            gopher_home: self.gopher_home(),
            search_engine: self.search_engine(),
            selector_prefix_enabled: self.selector_prefix_enabled(),
            default_charset: self.default_charset(),
            theme: self.theme.clone(),
        }
    }

    /// Applies updated settings, updates the live theme, and persists `config.ini` to disk.
    pub fn apply_settings(&mut self, settings: AppSettings) -> io::Result<()> {
        let home = settings.gopher_home.trim();
        let home = if home.is_empty() {
            DEFAULT_GOPHERHOME
        } else {
            home
        };
        let search_engine = settings.search_engine.trim();
        let search_engine = if search_engine.is_empty() {
            DEFAULT_SEARCH_ENGINE
        } else {
            search_engine
        };
        let charset = settings.default_charset.trim();
        let charset = if charset.is_empty() {
            DEFAULT_CHARSET
        } else {
            charset
        };

        self.config_file
            .set_setting("GOPHERHOME", home, "Navigation");
        self.config_file
            .set_setting("SEARCH_ENGINE", search_engine, "Navigation");
        self.config_file.set_setting(
            "SELECTOR_PREFIX_ENABLED",
            if settings.selector_prefix_enabled {
                "yes"
            } else {
                "no"
            },
            "Navigation",
        );
        self.config_file
            .set_setting("DEFAULT_CHARSET", charset, "Network");
        settings.theme.write_to_config(&mut self.config_file);
        self.theme = settings.theme;
        self.config_file.save()
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
