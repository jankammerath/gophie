//! Native Settings dialog window for editing Gophie configuration without touching `config.ini`.

use eframe::egui::{
    self, Button, Color32, ComboBox, CornerRadius, CursorIcon, FontId, Frame, Grid, Key, Margin,
    RichText, ScrollArea, Slider, Stroke, TextEdit, ViewportBuilder, ViewportId,
};

use crate::config::{AppSettings, Theme};

const COMMON_CHARSETS: &[&str] = &[
    "UTF-8",
    "ISO-8859-1",
    "Windows-1252",
    "Shift_JIS",
    "EUC-JP",
    "GBK",
];

/// Manages the native Settings window and its buffered draft configuration.
#[derive(Debug, Default)]
pub struct SettingsWindow {
    visible: bool,
    draft: AppSettings,
}

impl SettingsWindow {
    pub fn new() -> Self {
        Self::default()
    }

    /// Toggles visibility of the Settings window, refreshing the draft from `current` when opened.
    pub fn toggle(&mut self, current: AppSettings) {
        if self.visible {
            self.visible = false;
        } else {
            self.draft = current;
            self.visible = true;
        }
    }

    /// Renders the native Settings window if visible.
    /// Returns `Some(AppSettings)` when the user clicks Save.
    pub fn show(&mut self, ctx: &egui::Context, active_theme: &Theme) -> Option<AppSettings> {
        if !self.visible {
            return None;
        }

        let mut saved_settings = None;
        let mut close_requested = false;

        let viewport_id = ViewportId::from_hash_of("gophie_settings_window");
        let builder = ViewportBuilder::default()
            .with_title("Settings")
            .with_inner_size([520.0, 560.0])
            .with_min_inner_size([440.0, 420.0]);

        ctx.show_viewport_immediate(viewport_id, builder, |ctx, _class| {
            if ctx.input(|i| i.viewport().close_requested() || i.key_pressed(Key::Escape)) {
                close_requested = true;
            }

            // Bottom action bar in Gophie blue
            egui::TopBottomPanel::bottom("settings_action_bar")
                .frame(
                    Frame::new()
                        .fill(active_theme.actionbar_background)
                        .inner_margin(Margin::symmetric(16, 10)),
                )
                .show(ctx, |ui| {
                    ui.horizontal(|ui| {
                        let reset_btn = Self::bar_button(
                            "Reset Defaults",
                            active_theme.actionbar_inactive_text,
                            active_theme.actionbar_inactive_text,
                        );
                        if ui
                            .add(reset_btn)
                            .on_hover_cursor(CursorIcon::PointingHand)
                            .clicked()
                        {
                            self.draft = AppSettings::default();
                        }

                        ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                            let save_btn = Self::bar_button(
                                "Save",
                                active_theme.actionbar_text,
                                active_theme.actionbar_text,
                            );
                            if ui
                                .add(save_btn)
                                .on_hover_cursor(CursorIcon::PointingHand)
                                .clicked()
                            {
                                saved_settings = Some(self.draft.clone());
                                close_requested = true;
                            }

                            ui.add_space(8.0);

                            let cancel_btn = Self::bar_button(
                                "Cancel",
                                active_theme.actionbar_inactive_text,
                                active_theme.actionbar_inactive_text,
                            );
                            if ui
                                .add(cancel_btn)
                                .on_hover_cursor(CursorIcon::PointingHand)
                                .clicked()
                            {
                                close_requested = true;
                            }
                        });
                    });
                });

            // Scrollable settings body
            egui::CentralPanel::default()
                .frame(
                    Frame::new()
                        .fill(active_theme.view_background)
                        .inner_margin(Margin {
                            left: 16,
                            right: 6,
                            top: 14,
                            bottom: 14,
                        }),
                )
                .show(ctx, |ui| {
                    ui.style_mut().spacing.scroll = egui::style::ScrollStyle::solid();
                    let visuals = ui.visuals_mut();
                    visuals.extreme_bg_color = active_theme.view_background;
                    visuals.widgets.inactive.bg_stroke = Stroke::NONE;

                    let text_color = active_theme.view_text;
                    let accent = active_theme.page_link;
                    let input_bg = if active_theme.view_background.r() > 128 {
                        Color32::WHITE
                    } else {
                        Color32::from_rgb(0x2A, 0x2A, 0x2A)
                    };

                    ScrollArea::vertical()
                        .auto_shrink([false, false])
                        .show(ui, |ui| {
                            ui.visuals_mut().extreme_bg_color = input_bg;
                            ui.visuals_mut().widgets.inactive.bg_stroke =
                                Stroke::new(1.0_f32, text_color.gamma_multiply(0.25));

                            ui.add_space(2.0);
                            Self::section_heading(ui, "Navigation", accent);
                            Grid::new("settings_nav_grid")
                                .num_columns(2)
                                .spacing([16.0, 10.0])
                                .show(ui, |ui| {
                                    Self::label(ui, "Gopher Home", text_color);
                                    ui.add(
                                        TextEdit::singleline(&mut self.draft.gopher_home)
                                            .desired_width(280.0),
                                    );
                                    ui.end_row();

                                    Self::label(ui, "RFC 4266 Type Prefix", text_color);
                                    ui.checkbox(
                                        &mut self.draft.selector_prefix_enabled,
                                        RichText::new("Show /1/, /0/, etc. in address bar")
                                            .color(text_color),
                                    );
                                    ui.end_row();
                                });

                            ui.add_space(16.0);
                            Self::section_heading(ui, "Network", accent);
                            Grid::new("settings_net_grid")
                                .num_columns(2)
                                .spacing([16.0, 10.0])
                                .show(ui, |ui| {
                                    Self::label(ui, "Default Charset", text_color);
                                    ComboBox::from_id_salt("settings_charset_combo")
                                        .selected_text(&self.draft.default_charset)
                                        .width(180.0)
                                        .show_ui(ui, |ui| {
                                            for &cs in COMMON_CHARSETS {
                                                ui.selectable_value(
                                                    &mut self.draft.default_charset,
                                                    cs.to_string(),
                                                    cs,
                                                );
                                            }
                                        });
                                    ui.end_row();
                                });

                            ui.add_space(16.0);
                            Self::section_heading(ui, "Appearance", accent);

                            ui.horizontal(|ui| {
                                Self::label(ui, "Presets:", text_color);
                                ui.add_space(4.0);
                                if ui.button("Default (Dark)").clicked() {
                                    self.draft.theme = Theme::default();
                                }
                                if ui.button("Light").clicked() {
                                    self.draft.theme = Theme::light();
                                }
                                if ui.button("Grass").clicked() {
                                    self.draft.theme = Theme::grass();
                                }
                                if ui.button("Pink").clicked() {
                                    self.draft.theme = Theme::pink();
                                }
                            });

                            ui.add_space(10.0);
                            Grid::new("settings_font_grid")
                                .num_columns(2)
                                .spacing([16.0, 10.0])
                                .show(ui, |ui| {
                                    Self::label(ui, "Page Font Size", text_color);
                                    ui.add(
                                        Slider::new(&mut self.draft.theme.page_font_size, 10.0..=32.0)
                                            .step_by(1.0)
                                            .suffix(" px"),
                                    );
                                    ui.end_row();

                                    Self::label(ui, "Icon Font Size", text_color);
                                    ui.add(
                                        Slider::new(
                                            &mut self.draft.theme.page_icon_font_size,
                                            8.0..=24.0,
                                        )
                                        .step_by(1.0)
                                        .suffix(" px"),
                                    );
                                    ui.end_row();
                                });

                            ui.add_space(12.0);
                            Self::subsection_heading(ui, "Page Colors", text_color);
                            Grid::new("settings_page_colors")
                                .num_columns(4)
                                .spacing([14.0, 8.0])
                                .show(ui, |ui| {
                                    Self::color_row(
                                        ui,
                                        "Background",
                                        &mut self.draft.theme.view_background,
                                        text_color,
                                    );
                                    Self::color_row(
                                        ui,
                                        "Text",
                                        &mut self.draft.theme.view_text,
                                        text_color,
                                    );
                                    ui.end_row();

                                    Self::color_row(
                                        ui,
                                        "Links",
                                        &mut self.draft.theme.page_link,
                                        text_color,
                                    );
                                    Self::color_row(
                                        ui,
                                        "Line Numbers",
                                        &mut self.draft.theme.page_line_number,
                                        text_color,
                                    );
                                    ui.end_row();

                                    Self::color_row(
                                        ui,
                                        "Selection",
                                        &mut self.draft.theme.page_selection,
                                        text_color,
                                    );
                                    ui.end_row();
                                });

                            ui.add_space(12.0);
                            Self::subsection_heading(ui, "Navigation & Search Bar", text_color);
                            Grid::new("settings_nav_colors")
                                .num_columns(4)
                                .spacing([14.0, 8.0])
                                .show(ui, |ui| {
                                    Self::color_row(
                                        ui,
                                        "Bar Background",
                                        &mut self.draft.theme.nav_background,
                                        text_color,
                                    );
                                    Self::color_row(
                                        ui,
                                        "Bar Icons/Text",
                                        &mut self.draft.theme.nav_text,
                                        text_color,
                                    );
                                    ui.end_row();

                                    Self::color_row(
                                        ui,
                                        "Hover Text",
                                        &mut self.draft.theme.nav_text_hover,
                                        text_color,
                                    );
                                    Self::color_row(
                                        ui,
                                        "Search Background",
                                        &mut self.draft.theme.search_background,
                                        text_color,
                                    );
                                    ui.end_row();
                                });

                            ui.add_space(12.0);
                            Self::subsection_heading(ui, "Banners & Downloads", text_color);
                            Grid::new("settings_misc_colors")
                                .num_columns(4)
                                .spacing([14.0, 8.0])
                                .show(ui, |ui| {
                                    Self::color_row(
                                        ui,
                                        "Message Background",
                                        &mut self.draft.theme.message_background,
                                        text_color,
                                    );
                                    Self::color_row(
                                        ui,
                                        "Message Text",
                                        &mut self.draft.theme.message_foreground,
                                        text_color,
                                    );
                                    ui.end_row();

                                    Self::color_row(
                                        ui,
                                        "Downloads Bg",
                                        &mut self.draft.theme.filelist_background,
                                        text_color,
                                    );
                                    Self::color_row(
                                        ui,
                                        "Selected Download",
                                        &mut self.draft.theme.download_selected,
                                        text_color,
                                    );
                                    ui.end_row();
                                });

                            ui.add_space(8.0);
                        });
                });
        });

        if close_requested {
            self.visible = false;
        }
        saved_settings
    }

    fn section_heading(ui: &mut egui::Ui, title: &str, color: Color32) {
        ui.label(
            RichText::new(title)
                .font(FontId::proportional(15.0))
                .strong()
                .color(color),
        );
        ui.add_space(6.0);
    }

    fn subsection_heading(ui: &mut egui::Ui, title: &str, color: Color32) {
        ui.label(
            RichText::new(title)
                .font(FontId::proportional(13.0))
                .strong()
                .color(color.gamma_multiply(0.85)),
        );
        ui.add_space(4.0);
    }

    fn label(ui: &mut egui::Ui, text: &str, color: Color32) {
        ui.label(
            RichText::new(text)
                .font(FontId::proportional(13.0))
                .color(color),
        );
    }

    fn color_row(ui: &mut egui::Ui, label: &str, color: &mut Color32, text_color: Color32) {
        Self::label(ui, label, text_color);
        ui.color_edit_button_srgba(color);
    }

    fn bar_button(label: &str, text_color: Color32, border_color: Color32) -> Button<'_> {
        Button::new(
            RichText::new(label)
                .font(FontId::proportional(13.0))
                .color(text_color),
        )
        .fill(Color32::TRANSPARENT)
        .stroke(Stroke::new(1.0_f32, border_color))
        .corner_radius(CornerRadius::same(5))
    }
}
