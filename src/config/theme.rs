//! Visual theme configuration parsed from the `[Appearance]` section of `config.ini`.

use eframe::egui::{Color32, SystemTheme, ThemePreference};

use super::ConfigFile;

/// Controls the native OS window chrome (titlebar) appearance.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum WindowAppearance {
    #[default]
    System,
    Dark,
    Light,
}

impl WindowAppearance {
    /// Parses a `WINDOW_APPEARANCE` config value (`system`, `dark`, `light`).
    pub fn parse(value: &str) -> Self {
        match value.trim().to_ascii_lowercase().as_str() {
            "dark" => Self::Dark,
            "light" => Self::Light,
            _ => Self::System,
        }
    }

    /// Returns the string representation stored in `config.ini`.
    pub fn as_config_str(self) -> &'static str {
        match self {
            Self::System => "system",
            Self::Dark => "dark",
            Self::Light => "light",
        }
    }

    /// Converts to `egui::SystemTheme` for native OS window titlebar theming.
    pub fn to_system_theme(self) -> SystemTheme {
        match self {
            Self::System => SystemTheme::SystemDefault,
            Self::Dark => SystemTheme::Dark,
            Self::Light => SystemTheme::Light,
        }
    }

    /// Converts to `egui::ThemePreference` for built-in widget chrome.
    pub fn to_theme_preference(self) -> ThemePreference {
        match self {
            Self::System => ThemePreference::System,
            Self::Dark => ThemePreference::Dark,
            Self::Light => ThemePreference::Light,
        }
    }
}

/// Default palette and typography settings matching Gophie's signature dark-and-blue look.
#[derive(Debug, Clone)]
pub struct Theme {
    pub window_appearance: WindowAppearance,

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
    pub show_line_numbers: bool,
}

impl Default for Theme {
    fn default() -> Self {
        Self {
            window_appearance: WindowAppearance::Dark,

            nav_background: Color32::from_rgb(0x24, 0x8A, 0xC2),
            nav_text: Color32::from_rgb(0x76, 0xBC, 0xE3),
            nav_text_hover: Color32::WHITE,
            nav_selection: Color32::from_rgb(0x14, 0x52, 0x75),

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
            show_line_numbers: true,
        }
    }
}

impl Theme {
    /// Light theme preset inspired by Gophie's Light theme.
    pub fn light() -> Self {
        Self {
            window_appearance: WindowAppearance::Light,
            view_background: Color32::from_rgb(0xF7, 0xF7, 0xF7),
            view_text: Color32::from_rgb(0x1E, 0x1E, 0x1E),
            page_link: Color32::from_rgb(0x13, 0x8A, 0x3E),
            page_line_number: Color32::from_rgb(0xA0, 0xA0, 0xA0),
            page_selection: Color32::from_rgb(0xF2, 0xC9, 0x4C),
            filelist_background: Color32::from_rgb(0xF7, 0xF7, 0xF7),
            download_title: Color32::from_rgb(0x1E, 0x1E, 0x1E),
            download_text: Color32::from_rgb(0x55, 0x55, 0x55),
            ..Self::default()
        }
    }

    /// Grass theme preset inspired by Gophie's Grass theme.
    pub fn grass() -> Self {
        Self {
            nav_background: Color32::from_rgb(0x2E, 0x7D, 0x32),
            nav_text: Color32::from_rgb(0xA5, 0xD6, 0xA7),
            nav_selection: Color32::from_rgb(0x17, 0x47, 0x1A),
            actionbar_background: Color32::from_rgb(0x2E, 0x7D, 0x32),
            actionbar_inactive_text: Color32::from_rgb(0xA5, 0xD6, 0xA7),
            search_background: Color32::from_rgb(0x2E, 0x7D, 0x32),
            search_title: Color32::from_rgb(0xA5, 0xD6, 0xA7),
            view_background: Color32::from_rgb(0x15, 0x1D, 0x15),
            page_link: Color32::from_rgb(0x66, 0xBB, 0x6A),
            filelist_background: Color32::from_rgb(0x15, 0x1D, 0x15),
            ..Self::default()
        }
    }

    /// Pink theme preset inspired by Gophie's Pink theme.
    pub fn pink() -> Self {
        Self {
            nav_background: Color32::from_rgb(0xC2, 0x18, 0x5B),
            nav_text: Color32::from_rgb(0xF4, 0x8F, 0xB1),
            nav_selection: Color32::from_rgb(0x7A, 0x0D, 0x38),
            actionbar_background: Color32::from_rgb(0xC2, 0x18, 0x5B),
            actionbar_inactive_text: Color32::from_rgb(0xF4, 0x8F, 0xB1),
            search_background: Color32::from_rgb(0xC2, 0x18, 0x5B),
            search_title: Color32::from_rgb(0xF4, 0x8F, 0xB1),
            view_background: Color32::from_rgb(0x1D, 0x16, 0x1B),
            page_link: Color32::from_rgb(0xF0, 0x62, 0x92),
            filelist_background: Color32::from_rgb(0x1D, 0x16, 0x1B),
            ..Self::default()
        }
    }

    /// Writes all appearance values into the `[Appearance]` section of `config`.
    pub fn write_to_config(&self, config: &mut ConfigFile) {
        config.set_setting(
            "WINDOW_APPEARANCE",
            self.window_appearance.as_config_str(),
            "Appearance",
        );

        let mut set_color = |key: &str, c: Color32| {
            config.set_setting(key, &color_to_hex(c), "Appearance");
        };

        set_color("NAVIGATIONBAR_BACKGROUND", self.nav_background);
        set_color("NAVIGATIONBAR_TEXTCOLOR", self.nav_text);
        set_color("NAVIGATIONBAR_TEXTHOVERCOLOR", self.nav_text_hover);
        set_color("NAVIGATIONBAR_SELECTION_COLOR", self.nav_selection);

        set_color("VIEW_BACKGROUND", self.view_background);
        set_color("VIEW_TEXTCOLOR", self.view_text);
        set_color("PAGE_LINK_COLOR", self.page_link);
        set_color("PAGE_LINENUMBER_COLOR", self.page_line_number);
        set_color("PAGE_SELECTION_COLOR", self.page_selection);

        set_color("MESSAGEVIEW_BACKGROUND_COLOR", self.message_background);
        set_color("MESSAGEVIEW_FOREGROUND_COLOR", self.message_foreground);

        set_color("SEARCH_BACKGROUND", self.search_background);
        set_color("SEARCH_TITLECOLOR", self.search_title);
        set_color("SEARCH_TEXTCOLOR", self.search_text);

        set_color("ACTIONBAR_BACKGROUND", self.actionbar_background);
        set_color("ACTIONBAR_TEXTCOLOR", self.actionbar_text);
        set_color("ACTIONBAR_INACTIVE_TEXTCOLOR", self.actionbar_inactive_text);
        set_color("FILELIST_BACKGROUND", self.filelist_background);
        set_color("DOWNLOAD_SELECTED_COLOR", self.download_selected);
        set_color("DOWNLOAD_TITLE_COLOR", self.download_title);
        set_color("DOWNLOAD_TEXT_COLOR", self.download_text);

        config.set_setting(
            "PAGE_FONT_SIZE",
            &(self.page_font_size.round() as u32).to_string(),
            "Appearance",
        );
        config.set_setting(
            "PAGE_ICON_FONT_SIZE",
            &(self.page_icon_font_size.round() as u32).to_string(),
            "Appearance",
        );
        config.set_setting(
            "SHOW_LINE_NUMBERS",
            if self.show_line_numbers { "yes" } else { "no" },
            "Appearance",
        );
    }

    /// Builds a `Theme` by overriding defaults with any values present in `[Appearance]`.
    pub fn from_config(config: &ConfigFile) -> Self {
        let d = Self::default();
        let color = |key: &str, fallback: Color32| {
            let raw = config.get_setting(key, "Appearance", "");
            parse_hex_color(raw).unwrap_or(fallback)
        };

        let window_appearance = {
            let raw = config.get_setting("WINDOW_APPEARANCE", "Appearance", "");
            if raw.is_empty() {
                d.window_appearance
            } else {
                WindowAppearance::parse(raw)
            }
        };

        Self {
            window_appearance,
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
            show_line_numbers: config.get_bool(
                "SHOW_LINE_NUMBERS",
                "Appearance",
                d.show_line_numbers,
            ),
        }
    }
}

/// Formats an `egui::Color32` as a `#rrggbb` hex string for `config.ini`.
pub fn color_to_hex(color: Color32) -> String {
    format!("#{:02x}{:02x}{:02x}", color.r(), color.g(), color.b())
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
