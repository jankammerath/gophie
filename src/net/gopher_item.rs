//! Gopher menu item types and line parser (RFC 1436).

use super::gopher_url::{GopherUrl, DEFAULT_GOPHER_PORT};

/// Standard and commonly used non-canonical Gopher item types.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum GopherItemType {
    /// `0` - Plain text file
    TextFile,
    /// `1` - Gopher submenu
    GopherMenu,
    /// `2` - CCSO Nameserver
    CcsoNameserver,
    /// `3` - Error message
    ErrorCode,
    /// `4` - BinHex-encoded Macintosh file
    BinHexFile,
    /// `5` - DOS binary archive
    DosFile,
    /// `6` - uuencoded file
    UuencodedFile,
    /// `7` - Full-text search server
    FullTextSearch,
    /// `8` - Telnet session
    Telnet,
    /// `9` - Binary file
    BinaryFile,
    /// `+` - Mirror / alternate server
    Mirror,
    /// `g` - GIF image
    GifFile,
    /// `I` - Generic image file
    ImageFile,
    /// `T` - Telnet 3270 session
    Telnet3270,
    /// `h` - HTML file or `URL:` external link
    HtmlFile,
    /// `i` - Informational text line
    Information,
    /// `s` - Sound / audio file
    SoundFile,
    /// `?` - Unrecognized item type
    Unknown,
}

impl GopherItemType {
    /// Converts a single-character RFC 1436 type code into a `GopherItemType`.
    pub fn from_code(code: char) -> Self {
        match code {
            '0' => Self::TextFile,
            '1' => Self::GopherMenu,
            '2' => Self::CcsoNameserver,
            '3' => Self::ErrorCode,
            '4' => Self::BinHexFile,
            '5' => Self::DosFile,
            '6' => Self::UuencodedFile,
            '7' => Self::FullTextSearch,
            '8' => Self::Telnet,
            '9' => Self::BinaryFile,
            '+' => Self::Mirror,
            'g' => Self::GifFile,
            'I' => Self::ImageFile,
            'T' => Self::Telnet3270,
            'h' => Self::HtmlFile,
            'i' => Self::Information,
            's' => Self::SoundFile,
            _ => Self::Unknown,
        }
    }

    /// Returns the single-character RFC 1436 code for this item type.
    pub fn code(self) -> char {
        match self {
            Self::TextFile => '0',
            Self::GopherMenu => '1',
            Self::CcsoNameserver => '2',
            Self::ErrorCode => '3',
            Self::BinHexFile => '4',
            Self::DosFile => '5',
            Self::UuencodedFile => '6',
            Self::FullTextSearch => '7',
            Self::Telnet => '8',
            Self::BinaryFile => '9',
            Self::Mirror => '+',
            Self::GifFile => 'g',
            Self::ImageFile => 'I',
            Self::Telnet3270 => 'T',
            Self::HtmlFile => 'h',
            Self::Information => 'i',
            Self::SoundFile => 's',
            Self::Unknown => '?',
        }
    }

    /// Returns a human-readable description of this item type.
    pub fn display_name(self) -> &'static str {
        match self {
            Self::TextFile => "Text file",
            Self::GopherMenu => "Gopher menu",
            Self::CcsoNameserver => "CCSO Nameserver",
            Self::ErrorCode => "Error code",
            Self::BinHexFile => "BinHex file (Macintosh)",
            Self::DosFile => "DOS file",
            Self::UuencodedFile => "uuencoded file",
            Self::FullTextSearch => "Full-text search",
            Self::Telnet => "Telnet",
            Self::BinaryFile => "Binary file",
            Self::Mirror => "Mirror",
            Self::GifFile => "GIF file",
            Self::ImageFile => "Image file",
            Self::Telnet3270 => "Telnet 3270",
            Self::HtmlFile => "HTML file",
            Self::Information => "Information",
            Self::SoundFile => "Sound file",
            Self::Unknown => "Unknown",
        }
    }

    /// Returns the fallback file extension used when saving items without an explicit extension.
    pub fn default_file_ext(self) -> &'static str {
        match self {
            Self::TextFile => "txt",
            Self::GopherMenu => "gophermap",
            Self::CcsoNameserver => "ccso",
            Self::ErrorCode => "error",
            Self::BinHexFile => "hqx",
            Self::DosFile => "dat",
            Self::UuencodedFile => "uue",
            Self::FullTextSearch => "txt",
            Self::Telnet => "txt",
            Self::BinaryFile => "dat",
            Self::Mirror => "txt",
            Self::GifFile => "gif",
            Self::ImageFile => "jpg",
            Self::Telnet3270 => "txt",
            Self::HtmlFile => "htm",
            Self::Information => "txt",
            Self::SoundFile => "wav",
            Self::Unknown => "dat",
        }
    }

    /// Returns `true` if this item type should be handled as a downloadable binary/media file.
    pub fn is_binary(self) -> bool {
        matches!(
            self,
            Self::BinHexFile
                | Self::DosFile
                | Self::UuencodedFile
                | Self::BinaryFile
                | Self::SoundFile
        )
    }

    /// Returns `true` if this item type is an image rendered inline by Gophie.
    pub fn is_image(self) -> bool {
        matches!(self, Self::ImageFile | Self::GifFile)
    }
}

/// A single entry in a Gopher menu or a navigable Gopher resource target.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GopherItem {
    item_type: GopherItemType,
    item_type_code: char,
    display_text: String,
    selector: String,
    host: String,
    port: u16,
}

impl Default for GopherItem {
    fn default() -> Self {
        Self {
            item_type: GopherItemType::Unknown,
            item_type_code: '?',
            display_text: String::new(),
            selector: String::new(),
            host: String::new(),
            port: DEFAULT_GOPHER_PORT,
        }
    }
}

impl GopherItem {
    /// Parses a single tab-delimited RFC 1436 menu line into a `GopherItem`.
    pub fn parse_line(line: &str) -> Self {
        let cleaned = line.trim_end_matches(['\r', '\n']);
        let mut chars = cleaned.chars();
        let Some(first_char) = chars.next() else {
            return Self::default();
        };

        let item_type = GopherItemType::from_code(first_char);
        let rest = chars.as_str();
        let mut fields = rest.split('\t');

        let display_text = fields.next().unwrap_or("").to_string();
        let selector = fields.next().unwrap_or("").trim().to_string();
        let host = fields.next().unwrap_or("").trim().to_string();
        let port = fields
            .next()
            .and_then(|p| p.trim().parse::<u16>().ok())
            .unwrap_or(DEFAULT_GOPHER_PORT);

        Self {
            item_type,
            item_type_code: first_char,
            display_text,
            selector,
            host,
            port,
        }
    }

    /// Constructs a `GopherItem` from a known type and `GopherUrl`.
    pub fn from_url(item_type: GopherItemType, url: &GopherUrl) -> Self {
        let mut item = Self {
            item_type,
            item_type_code: item_type.code(),
            display_text: String::new(),
            selector: url.selector().to_string(),
            host: url.host().to_string(),
            port: url.port(),
        };
        item.display_text = item.file_name();
        item
    }

    /// Constructs a `GopherItem` from a single-character type code and `GopherUrl`.
    pub fn from_code_and_url(code: char, url: &GopherUrl) -> Self {
        Self::from_url(GopherItemType::from_code(code), url)
    }

    pub fn item_type(&self) -> GopherItemType {
        self.item_type
    }

    pub fn item_type_code(&self) -> char {
        self.item_type_code
    }

    pub fn display_text(&self) -> &str {
        &self.display_text
    }

    pub fn selector(&self) -> &str {
        &self.selector
    }

    pub fn host(&self) -> &str {
        &self.host
    }

    pub fn port(&self) -> u16 {
        self.port
    }

    /// Returns `true` if this item should be rendered as a clickable link.
    pub fn is_link(&self) -> bool {
        !matches!(
            self.item_type,
            GopherItemType::Information | GopherItemType::Unknown | GopherItemType::ErrorCode
        )
    }

    /// Returns `true` if this item is a downloadable binary or sound file.
    pub fn is_binary_file(&self) -> bool {
        self.item_type.is_binary()
    }

    /// Builds the target URL string for this item (including external `URL:` selectors).
    pub fn url_string(&self) -> String {
        if matches!(
            self.item_type,
            GopherItemType::Unknown | GopherItemType::Information
        ) {
            return String::new();
        }

        if let Some(ext) = self.selector.strip_prefix("/URL:") {
            return ext.to_string();
        }
        if let Some(ext) = self.selector.strip_prefix("URL:") {
            return ext.to_string();
        }

        let mut url = format!("gopher://{}", self.host);
        if self.port != DEFAULT_GOPHER_PORT {
            url.push(':');
            url.push_str(&self.port.to_string());
        }
        if !self.selector.starts_with('/') {
            url.push('/');
        }
        url.push_str(&self.selector);
        url
    }

    /// Extracts the trailing file name component from the item's URL.
    pub fn file_name(&self) -> String {
        let url = self.url_string();
        match url.rfind('/') {
            Some(idx) if idx + 1 < url.len() => url[idx + 1..].to_string(),
            _ => url,
        }
    }

    /// Returns the file name, appending a default extension if none is present.
    pub fn file_name_with_forced_ext(&self) -> String {
        let mut name = self.file_name();
        if name.is_empty() {
            name.push_str("download");
        }
        if !name.contains('.') {
            name.push('.');
            name.push_str(self.item_type.default_file_ext());
        }
        name
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_gophermap_line() {
        let line = "1Super-Dimensional Fortress: SDF Gopherspace\t/\tsdf.org\t70\r\n";
        let item = GopherItem::parse_line(line);
        assert_eq!(item.item_type(), GopherItemType::GopherMenu);
        assert_eq!(
            item.display_text(),
            "Super-Dimensional Fortress: SDF Gopherspace"
        );
        assert_eq!(item.selector(), "/");
        assert_eq!(item.host(), "sdf.org");
        assert_eq!(item.port(), 70);
        assert_eq!(item.url_string(), "gopher://sdf.org/");
    }

    #[test]
    fn parses_external_url_selector() {
        let line = "hGophie Website\tURL:https://gophie.org/\tgopher.floodgap.com\t70";
        let item = GopherItem::parse_line(line);
        assert_eq!(item.item_type(), GopherItemType::HtmlFile);
        assert_eq!(item.url_string(), "https://gophie.org/");
    }
}
