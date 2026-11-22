//! Visual theme configuration parsed from the `[Appearance]` section of `config.ini`.

use eframe::egui::Color32;

use super::ConfigFile;

/// Default palette and typography settings matching Gophie's signature dark-and-blue look.
#[derive(Debug, Clone)]
pub struct Theme {
    pub nav_background: Color32,
    pub nav_text: Color32,
    pub nav_text_hover: Color32,
    pub nav_selection: Color32,

    pub view_background: Color32,
    pub view_text: Color32,
    pub page_link: Color32,
    pub page_line_number: Color32,
    pub page_selection: Color32,

    pub message_background: Color32,
    pub message_foreground: Color32,

    pub search_background: Color32,
    pub search_title: Color32,
    pub search_text: Color32,

    pub actionbar_background: Color32,
    pub actionbar_text: Color32,
    pub actionbar_inactive_text: Color32,
    pub filelist_background: Color32,
    pub download_selected: Color32,
    pub download_title: Color32,
    pub download_text: Color32,

    pub page_font_size: f32,
    pub page_icon_font_size: f32,
}

impl Default for Theme {
    fn default() -> Self {
        Self {
            nav_background: Color32::from_rgb(0x24, 0x8A, 0xC2),
            nav_text: Color32::from_rgb(0x76, 0xBC, 0xE3),
            nav_text_hover: Color32::WHITE,
            nav_selection: Color32::WHITE,

            view_background: Color32::from_rgb(0x1B, 0x1B, 0x1B),
            view_text: Color32::from_rgb(0xE8, 0xE8, 0xE8),
            page_link: Color32::from_rgb(0x22, 0xC7, 0x5C),
            page_line_number: Color32::from_rgb(0x45, 0x45, 0x45),
            page_selection: Color32::from_rgb(0xCF, 0x9A, 0x0C),

            message_background: Color32::from_rgb(0xFC, 0xBA, 0x03),
            message_foreground: Color32::BLACK,

            search_background: Color32::from_rgb(0x24, 0x8A, 0xC2),
            search_title: Color32::from_rgb(0x76, 0xBC, 0xE3),
            search_text: Color32::from_rgb(0xE8, 0xE8, 0xE8),

            actionbar_background: Color32::from_rgb(0x24, 0x8A, 0xC2),
            actionbar_text: Color32::WHITE,
            actionbar_inactive_text: Color32::from_rgb(0x76, 0xBC, 0xE3),
            filelist_background: Color32::from_rgb(0x1B, 0x1B, 0x1B),
            download_selected: Color32::from_rgb(0xCF, 0x9A, 0x0C),
            download_title: Color32::WHITE,
            download_text: Color32::from_rgb(0xE0, 0xE0, 0xE0),

            page_font_size: 17.0,
            page_icon_font_size: 12.0,
        }
    }
}

impl Theme {
    /// Builds a `Theme` by overriding defaults with any values present in `[Appearance]`.
    pub fn from_config(config: &ConfigFile) -> Self {
        let d = Self::default();
        let color = |key: &str, fallback: Color32| {
            let raw = config.get_setting(key, "Appearance", "");
            parse_hex_color(raw).unwrap_or(fallback)
        };

        Self {
            nav_background: color("NAVIGATIONBAR_BACKGROUND", d.nav_background),
            nav_text: color("NAVIGATIONBAR_TEXTCOLOR", d.nav_text),
            nav_text_hover: color("NAVIGATIONBAR_TEXTHOVERCOLOR", d.nav_text_hover),
            nav_selection: color("NAVIGATIONBAR_SELECTION_COLOR", d.nav_selection),

            view_background: color("VIEW_BACKGROUND", d.view_background),
            view_text: color("VIEW_TEXTCOLOR", d.view_text),
            page_link: color("PAGE_LINK_COLOR", d.page_link),
            page_line_number: color("PAGE_LINENUMBER_COLOR", d.page_line_number),
            page_selection: color("PAGE_SELECTION_COLOR", d.page_selection),

            message_background: color("MESSAGEVIEW_BACKGROUND_COLOR", d.message_background),
            message_foreground: color("MESSAGEVIEW_FOREGROUND_COLOR", d.message_foreground),

            search_background: color("SEARCH_BACKGROUND", d.search_background),
            search_title: color("SEARCH_TITLECOLOR", d.search_title),
            search_text: color("SEARCH_TEXTCOLOR", d.search_text),

            actionbar_background: color("ACTIONBAR_BACKGROUND", d.actionbar_background),
            actionbar_text: color("ACTIONBAR_TEXTCOLOR", d.actionbar_text),
            actionbar_inactive_text: color("ACTIONBAR_INACTIVE_TEXTCOLOR", d.actionbar_inactive_text),
            filelist_background: color("FILELIST_BACKGROUND", d.filelist_background),
            download_selected: color("DOWNLOAD_SELECTED_COLOR", d.download_selected),
            download_title: color("DOWNLOAD_TITLE_COLOR", d.download_title),
            download_text: color("DOWNLOAD_TEXT_COLOR", d.download_text),

            page_font_size: config
                .get_f32("PAGE_FONT_SIZE", "Appearance", d.page_font_size)
                .clamp(8.0, 48.0),
            page_icon_font_size: config
                .get_f32("PAGE_ICON_FONT_SIZE", "Appearance", d.page_icon_font_size)
                .clamp(8.0, 36.0),
        }
    }
}

/// Parses a `#RRGGBB` or `#RGB` hex color string into an `egui::Color32`.
pub fn parse_hex_color(hex: &str) -> Option<Color32> {
    let trimmed = hex.trim().strip_prefix('#').unwrap_or(hex.trim());
    match trimmed.len() {
        6 => {
            let r = u8::from_str_radix(&trimmed[0..2], 16).ok()?;
            let g = u8::from_str_radix(&trimmed[2..4], 16).ok()?;
            let b = u8::from_str_radix(&trimmed[4..6], 16).ok()?;
            Some(Color32::from_rgb(r, g, b))
        }
        3 => {
            let r = u8::from_str_radix(&trimmed[0..1], 16).ok()? * 17;
            let g = u8::from_str_radix(&trimmed[1..2], 16).ok()? * 17;
            let b = u8::from_str_radix(&trimmed[2..3], 16).ok()? * 17;
            Some(Color32::from_rgb(r, g, b))
        }
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_hex_colors() {
        assert_eq!(parse_hex_color("#248AC2"), Some(Color32::from_rgb(0x24, 0x8A, 0xC2)));
        assert_eq!(parse_hex_color("#1b1b1b"), Some(Color32::from_rgb(0x1B, 0x1B, 0x1B)));
        assert_eq!(parse_hex_color("#fff"), Some(Color32::WHITE));
        assert_eq!(parse_hex_color("invalid"), None);
    }
}
