//! Gopher page model holding raw response bytes, decoded text, and parsed menu items.

use std::fs;
use std::io;
use std::path::Path;

use encoding_rs::Encoding;

use super::{GopherItem, GopherItemType, GopherUrl};

/// Represents a fetched Gopher resource (menu, text document, or image).
#[derive(Debug, Clone)]
pub struct GopherPage {
    raw_bytes: Vec<u8>,
    url: GopherUrl,
    items: Vec<GopherItem>,
    content_type: GopherItemType,
    charset: String,
    decoded_text: String,
}

impl GopherPage {
    /// Builds a `GopherPage` from raw server bytes and parses menu entries when appropriate.
    pub fn new(
        raw_bytes: Vec<u8>,
        expected_type: GopherItemType,
        url: GopherUrl,
        charset: impl Into<String>,
    ) -> Self {
        let charset = charset.into();
        let decoded_text = if expected_type.is_image() {
            String::new()
        } else {
            Self::decode_bytes(&raw_bytes, &charset)
        };

        let mut items = Vec::new();
        let content_type = match expected_type {
            GopherItemType::GopherMenu => {
                items = Self::parse_menu(&decoded_text);
                GopherItemType::GopherMenu
            }
            GopherItemType::Unknown => {
                let parsed = Self::parse_menu(&decoded_text);
                let looks_like_menu = decoded_text.lines().any(|l| l.contains('\t'));
                if looks_like_menu && !parsed.is_empty() {
                    items = parsed;
                    GopherItemType::GopherMenu
                } else {
                    GopherItemType::TextFile
                }
            }
            other => other,
        };

        Self {
            raw_bytes,
            url,
            items,
            content_type,
            charset,
            decoded_text,
        }
    }

    /// Returns the resolved content type of this page.
    pub fn content_type(&self) -> GopherItemType {
        self.content_type
    }

    /// Returns the raw payload bytes received from the Gopher server.
    pub fn raw_bytes(&self) -> &[u8] {
        &self.raw_bytes
    }

    /// Returns the URL of this page.
    pub fn url(&self) -> &GopherUrl {
        &self.url
    }

    /// Returns the parsed menu items (populated when `content_type == GopherMenu`).
    pub fn items(&self) -> &[GopherItem] {
        &self.items
    }

    /// Returns the decoded source text of this page.
    pub fn source_code(&self) -> &str {
        &self.decoded_text
    }

    /// Returns the charset used to decode this page.
    pub fn charset(&self) -> &str {
        &self.charset
    }

    /// Returns the human-readable plain text of this page (display strings for menus,
    /// or terminator-stripped text for documents).
    pub fn text_content(&self) -> String {
        if !self.items.is_empty() {
            let mut out = String::new();
            for item in &self.items {
                out.push_str(item.display_text());
                out.push('\n');
            }
            out
        } else {
            self.decoded_text
                .replace("\r\n.\r\n", "")
                .replace("\n.\r\n", "")
                .replace("\n.\n", "")
        }
    }

    /// Suggests a default file name with extension when saving this page to disk.
    pub fn file_name(&self) -> String {
        let url_str = self.url.to_url_string(false);
        let mut name = match url_str.rfind('/') {
            Some(idx) if idx + 1 < url_str.len() => url_str[idx + 1..].to_string(),
            _ => String::from("index"),
        };

        if !name.contains('.') {
            name.push('.');
            name.push_str(self.content_type.default_file_ext());
        }
        name
    }

    /// Writes the raw payload bytes to `path`.
    pub fn save_as_file(&self, path: &Path) -> io::Result<()> {
        fs::write(path, &self.raw_bytes)
    }

    fn decode_bytes(bytes: &[u8], charset: &str) -> String {
        let encoding = Encoding::for_label(charset.as_bytes()).unwrap_or(encoding_rs::UTF_8);
        let (cow, _, _) = encoding.decode(bytes);
        cow.into_owned()
    }

    fn parse_menu(source: &str) -> Vec<GopherItem> {
        let mut items = Vec::new();
        for raw_line in source.split('\n') {
            let trimmed = raw_line.trim_end_matches('\r');
            if trimmed.is_empty() || trimmed == "." {
                continue;
            }
            items.push(GopherItem::parse_line(trimmed));
        }
        items
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_gophermap_and_ignores_terminator() {
        let raw = b"iWelcome to Floodgap\tfake\terror.host\t1\r\n1Fun\t/fun\tgopher.floodgap.com\t70\r\n.\r\n";
        let url = GopherUrl::parse("gopher.floodgap.com");
        let page = GopherPage::new(raw.to_vec(), GopherItemType::GopherMenu, url, "UTF-8");

        assert_eq!(page.content_type(), GopherItemType::GopherMenu);
        assert_eq!(page.items().len(), 2);
        assert_eq!(page.items()[0].display_text(), "Welcome to Floodgap");
        assert_eq!(page.items()[1].item_type(), GopherItemType::GopherMenu);
        assert_eq!(page.file_name(), "index.gophermap");
        assert_eq!(page.text_content(), "Welcome to Floodgap\nFun\n");
    }

    #[test]
    fn falls_back_to_text_for_unknown_plain_document() {
        let raw = b"Plain text document\r\nLine 2\r\n.\r\n";
        let url = GopherUrl::parse("gopher.floodgap.com/readme");
        let page = GopherPage::new(raw.to_vec(), GopherItemType::Unknown, url, "UTF-8");

        assert_eq!(page.content_type(), GopherItemType::TextFile);
        assert!(page.items().is_empty());
        assert_eq!(page.text_content(), "Plain text document\r\nLine 2");
        assert_eq!(page.file_name(), "readme.txt");
    }
}
