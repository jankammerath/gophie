//! INI configuration file parser and serializer compatible with Gophie's `config.ini`.

use std::collections::BTreeMap;
use std::fs;
use std::io;
use std::path::{Path, PathBuf};

/// Represents an INI configuration file organized into `[Section]` blocks and `KEY = VALUE` pairs.
#[derive(Debug, Clone)]
pub struct ConfigFile {
    path: PathBuf,
    sections: BTreeMap<String, BTreeMap<String, String>>,
}

impl ConfigFile {
    /// Loads and parses an INI configuration file from `path`.
    /// If the file does not exist yet, an empty configuration is returned.
    pub fn load(path: impl Into<PathBuf>) -> Self {
        let path = path.into();
        let mut config = Self {
            path,
            sections: BTreeMap::new(),
        };
        config.reload();
        config
    }

    /// Creates an in-memory `ConfigFile` from raw INI text (useful for testing).
    pub fn from_ini_str(content: &str, path: impl Into<PathBuf>) -> Self {
        let mut config = Self {
            path: path.into(),
            sections: BTreeMap::new(),
        };
        config.parse_str(content);
        config
    }

    /// Returns the filesystem path associated with this configuration file.
    pub fn path(&self) -> &Path {
        &self.path
    }

    /// Reads a setting from `section`, falling back to `default` if absent.
    pub fn get_setting<'a>(&'a self, name: &str, section: &str, default: &'a str) -> &'a str {
        self.sections
            .get(section)
            .and_then(|map| map.get(name))
            .map(String::as_str)
            .unwrap_or(default)
    }

    /// Reads a boolean setting (`yes`/`true`/`1` vs `no`/`false`/`0`).
    pub fn get_bool(&self, name: &str, section: &str, default: bool) -> bool {
        let default_str = if default { "yes" } else { "no" };
        match self
            .get_setting(name, section, default_str)
            .to_ascii_lowercase()
            .as_str()
        {
            "yes" | "true" | "1" | "on" => true,
            "no" | "false" | "0" | "off" => false,
            _ => default,
        }
    }

    /// Reads a floating-point setting, falling back to `default` on parse failure.
    pub fn get_f32(&self, name: &str, section: &str, default: f32) -> f32 {
        self.get_setting(name, section, "")
            .parse::<f32>()
            .unwrap_or(default)
    }

    /// Sets or updates a configuration value in `section`.
    /// Empty section names, keys, or values are ignored to match original behavior.
    pub fn set_setting(&mut self, name: &str, value: &str, section: &str) {
        if section.is_empty() || name.is_empty() || value.is_empty() {
            return;
        }
        self.sections
            .entry(section.to_string())
            .or_default()
            .insert(name.to_string(), value.to_string());
    }

    /// Serializes all sections and key-value pairs into INI text format.
    pub fn to_ini_string(&self) -> String {
        let mut output = String::new();
        for (section, entries) in &self.sections {
            if !output.is_empty() {
                output.push('\n');
            }
            output.push('[');
            output.push_str(section);
            output.push_str("]\n");
            for (key, value) in entries {
                output.push_str(key);
                output.push_str(" = ");
                output.push_str(value);
                output.push('\n');
            }
        }
        output
    }

    /// Writes the configuration file to disk, creating parent directories if needed.
    pub fn save(&self) -> io::Result<()> {
        if let Some(parent) = self.path.parent() {
            fs::create_dir_all(parent)?;
        }
        fs::write(&self.path, self.to_ini_string())
    }

    /// Reloads and re-parses the configuration file from disk if it exists.
    pub fn reload(&mut self) {
        self.sections.clear();
        if !self.path.exists() {
            return;
        }
        match fs::read_to_string(&self.path) {
            Ok(content) => self.parse_str(&content),
            Err(err) => eprintln!(
                "Failed to read config file ({}): {}",
                self.path.display(),
                err
            ),
        }
    }

    /// Parses INI text into the internal section map.
    fn parse_str(&mut self, content: &str) {
        let mut current_section = String::from("NONE");
        for raw_line in content.lines() {
            let line = raw_line.trim();
            if line.is_empty() || line.starts_with(';') || line.starts_with('#') {
                continue;
            }
            if line.starts_with('[') && line.ends_with(']') && line.len() >= 2 {
                current_section = line[1..line.len() - 1].trim().to_string();
                continue;
            }
            if let Some((key, value)) = line.split_once('=') {
                self.set_setting(key.trim(), value.trim(), &current_section);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_sections_comments_and_values() {
        let ini = r#"
; Comment line
[Navigation]
GOPHERHOME = gopher.floodgap.com
SELECTOR_PREFIX_ENABLED = yes

[Appearance]
VIEW_BACKGROUND = #1b1b1b
PAGE_FONT_SIZE = 17
"#;
        let cfg = ConfigFile::from_ini_str(ini, "test.ini");
        assert_eq!(
            cfg.get_setting("GOPHERHOME", "Navigation", ""),
            "gopher.floodgap.com"
        );
        assert!(cfg.get_bool("SELECTOR_PREFIX_ENABLED", "Navigation", false));
        assert_eq!(cfg.get_setting("VIEW_BACKGROUND", "Appearance", ""), "#1b1b1b");
        assert_eq!(cfg.get_f32("PAGE_FONT_SIZE", "Appearance", 12.0), 17.0);
        assert_eq!(cfg.get_setting("MISSING", "Appearance", "fallback"), "fallback");
    }

    #[test]
    fn round_trips_to_ini_string() {
        let mut cfg = ConfigFile::from_ini_str("", "test.ini");
        cfg.set_setting("GOPHERHOME", "gopher.quux.org", "Navigation");
        cfg.set_setting("DEFAULT_CHARSET", "UTF-8", "Network");

        let serialized = cfg.to_ini_string();
        let reparsed = ConfigFile::from_ini_str(&serialized, "test.ini");
        assert_eq!(
            reparsed.get_setting("GOPHERHOME", "Navigation", ""),
            "gopher.quux.org"
        );
        assert_eq!(
            reparsed.get_setting("DEFAULT_CHARSET", "Network", ""),
            "UTF-8"
        );
    }
}
