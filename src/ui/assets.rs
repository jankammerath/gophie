//! Embedded fonts, Feather icon glyph constants, and window icon decoding.

use std::sync::Arc;

use eframe::egui::{self, FontData, FontDefinitions, FontFamily, FontId, IconData};

const FONT_INCONSOLATA: &[u8] = include_bytes!("../../res/Inconsolata-Regular.ttf");
const FONT_OPEN_SANS: &[u8] = include_bytes!("../../res/OpenSans-Regular.ttf");
const FONT_FEATHER: &[u8] = include_bytes!("../../res/Feather.ttf");
const APP_ICON_PNG: &[u8] = include_bytes!("../../res/icon.png");

/// Named font family for the embedded Feather icon font.
pub const FEATHER_FAMILY: &str = "feather";

/// Feather icon font codepoints used throughout Gophie's UI.
pub mod icons {
    use crate::net::GopherItemType;

    pub const BACK: &str = "\u{E910}";
    pub const FORWARD: &str = "\u{E912}";
    pub const REFRESH: &str = "\u{E9B9}";
    pub const STOP: &str = "\u{E9D6}";
    pub const HOME: &str = "\u{E979}";
    pub const DOWNLOADS: &str = "\u{E90B}";
    pub const SEARCH: &str = "\u{E9C1}";

    pub const INFO: &str = "\u{E902}";
    pub const CONFIRM: &str = "\u{E904}";

    pub const CLEAR_LIST: &str = "\u{E9E3}";
    pub const ABORT: &str = "\u{EA02}";
    pub const RETRY: &str = "\u{E9B9}";
    pub const OPEN: &str = "\u{E9C5}";
    pub const START: &str = "\u{E913}";

    /// Returns the Feather icon glyph corresponding to a Gopher item type.
    pub fn for_item_type(item_type: GopherItemType) -> &'static str {
        match item_type {
            GopherItemType::TextFile => "\u{E964}",
            GopherItemType::GopherMenu => "\u{E90F}",
            GopherItemType::CcsoNameserver => "\u{E951}",
            GopherItemType::ErrorCode => "\u{E97C}",
            GopherItemType::BinHexFile
            | GopherItemType::DosFile
            | GopherItemType::UuencodedFile
            | GopherItemType::BinaryFile => "\u{E9BF}",
            GopherItemType::FullTextSearch => "\u{E9C1}",
            GopherItemType::Telnet | GopherItemType::Telnet3270 => "\u{E9DD}",
            GopherItemType::Mirror => "\u{E9C3}",
            GopherItemType::GifFile | GopherItemType::ImageFile => "\u{E97A}",
            GopherItemType::HtmlFile => "\u{E972}",
            GopherItemType::Information => "",
            GopherItemType::SoundFile => "\u{E99E}",
            GopherItemType::Unknown => "\u{E9D0}",
        }
    }
}

/// Returns a `FontId` for the embedded Feather icon font at `size` points.
pub fn feather_font(size: f32) -> FontId {
    FontId::new(size, FontFamily::Name(Arc::from(FEATHER_FAMILY)))
}

/// Registers Gophie's embedded fonts (OpenSans, Inconsolata, Feather) with `egui`.
pub fn configure_fonts(ctx: &egui::Context) {
    let mut fonts = FontDefinitions::default();

    fonts.font_data.insert(
        "open_sans".to_owned(),
        Arc::new(FontData::from_static(FONT_OPEN_SANS)),
    );
    fonts.font_data.insert(
        "inconsolata".to_owned(),
        Arc::new(FontData::from_static(FONT_INCONSOLATA)),
    );
    fonts.font_data.insert(
        FEATHER_FAMILY.to_owned(),
        Arc::new(FontData::from_static(FONT_FEATHER)),
    );

    fonts
        .families
        .entry(FontFamily::Proportional)
        .or_default()
        .insert(0, "open_sans".to_owned());

    fonts
        .families
        .entry(FontFamily::Monospace)
        .or_default()
        .insert(0, "inconsolata".to_owned());

    fonts.families.insert(
        FontFamily::Name(Arc::from(FEATHER_FAMILY)),
        vec![FEATHER_FAMILY.to_owned()],
    );

    ctx.set_fonts(fonts);
}

/// Decodes the embedded PNG application icon for the OS window chrome.
pub fn load_window_icon() -> Option<Arc<IconData>> {
    let img = image::load_from_memory(APP_ICON_PNG).ok()?.into_rgba8();
    let (width, height) = img.dimensions();
    Some(Arc::new(IconData {
        rgba: img.into_raw(),
        width,
        height,
    }))
}
