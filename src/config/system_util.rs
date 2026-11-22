//! Cross-platform system utilities for formatting sizes and launching external apps.

use std::path::Path;

/// Collection of stateless cross-platform helper utilities.
pub struct SystemUtility;

impl SystemUtility {
    /// Formats a byte count into a human-readable string (`Byte`, `KB`, `MB`, `GB`).
    pub fn format_file_size(bytes: u64) -> String {
        const KB: f64 = 1_000.0;
        const MB: f64 = 1_000_000.0;
        const GB: f64 = 1_000_000_000.0;

        let size = bytes as f64;
        if size >= GB {
            format!("{:.2} GB", size / GB)
        } else if size >= MB {
            format!("{:.2} MB", size / MB)
        } else if size >= KB {
            format!("{:.2} KB", size / KB)
        } else {
            format!("{} Byte", bytes)
        }
    }

    /// Opens a URI (`http://`, `https://`, `mailto:`, `telnet://`) with the OS default handler.
    pub fn open_uri(uri: &str) -> Result<(), String> {
        open::that_detached(uri).map_err(|err| format!("Unable to open '{}': {}", uri, err))
    }

    /// Opens a local file with the OS default application.
    pub fn open_file(path: &Path) -> Result<(), String> {
        open::that_detached(path)
            .map_err(|err| format!("Unable to open '{}': {}", path.display(), err))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn formats_byte_sizes() {
        assert_eq!(SystemUtility::format_file_size(512), "512 Byte");
        assert_eq!(SystemUtility::format_file_size(5_620), "5.62 KB");
        assert_eq!(SystemUtility::format_file_size(73_580_000), "73.58 MB");
        assert_eq!(SystemUtility::format_file_size(2_500_000_000), "2.50 GB");
    }
}
