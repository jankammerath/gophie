//! Native Settings dialog window for editing Gophie configuration without touching `config.ini`.

use eframe::egui::{
    self, Button, Color32, ComboBox, CornerRadius, CursorIcon, FontId, Frame, Grid, Key, Margin,
    RichText, ScrollArea, Slider, Stroke, TextEdit, Vec2, ViewportBuilder, ViewportCommand,
    ViewportId,
};

use crate::config::{AppSettings, NavigationBarPosition, Theme, WindowAppearance};

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
    applied_appearance: Option<WindowAppearance>,
}

impl SettingsWindow {
    pub fn new() -> Self {
        Self::default()
    }

    /// Toggles visibility of the Settings window, refreshing the draft from `current` when opened.
    pub fn toggle(&mut self, current: AppSettings) {
        if self.visible {
            self.visible = false;
            self.applied_appearance = None;
        } else {
            self.draft = current;
            self.applied_appearance = None;
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
            .with_inner_size([560.0, 620.0])
            .with_min_inner_size([480.0, 460.0]);

        ctx.show_viewport_immediate(viewport_id, builder, |ctx, _class| {
            if self.applied_appearance != Some(active_theme.window_appearance) {
                ctx.send_viewport_cmd(ViewportCommand::SetTheme(
                    active_theme.window_appearance.to_system_theme(),
                ));
                self.applied_appearance = Some(active_theme.window_appearance);
            }

            if ctx.input(|i| i.viewport().close_requested() || i.key_pressed(Key::Escape)) {
                close_requested = true;
            }

            // Bottom action bar in Gophie blue with comfortable button padding
            egui::TopBottomPanel::bottom("settings_action_bar")
                .frame(
                    Frame::new()
                        .fill(active_theme.actionbar_background)
                        .inner_margin(Margin::symmetric(18, 12)),
                )
                .show(ctx, |ui| {
                    ui.spacing_mut().button_padding = Vec2::new(16.0, 7.0);
                    ui.spacing_mut().item_spacing.x = 10.0;

                    ui.horizontal(|ui| {
                        let reset_btn = Self::bar_button(
                            "Reset Defaults",
                            active_theme.actionbar_inactive_text,
                            active_theme.actionbar_inactive_text,
                            Color32::TRANSPARENT,
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
                                Color32::from_white_alpha(28),
                            );
                            if ui
                                .add(save_btn)
                                .on_hover_cursor(CursorIcon::PointingHand)
                                .clicked()
                            {
                                saved_settings = Some(self.draft.clone());
                                close_requested = true;
                            }

                            let cancel_btn = Self::bar_button(
                                "Cancel",
                                active_theme.actionbar_inactive_text,
                                active_theme.actionbar_inactive_text,
                                Color32::TRANSPARENT,
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
                            left: 20,
                            right: 8,
                            top: 18,
                            bottom: 18,
                        }),
                )
                .show(ctx, |ui| {
                    ui.style_mut().spacing.scroll = egui::style::ScrollStyle::solid();
                    let visuals = ui.visuals_mut();
                    visuals.extreme_bg_color = active_theme.view_background;
                    visuals.widgets.inactive.bg_stroke = Stroke::NONE;

                    let is_light = active_theme.view_background.r() > 128;
                    let text_color = active_theme.view_text;
                    let muted_color = text_color.gamma_multiply(0.65);
                    let accent = active_theme.page_link;
                    let card_bg = if is_light {
                        Color32::from_rgb(0xFF, 0xFF, 0xFF)
                    } else {
                        Color32::from_rgb(0x23, 0x23, 0x23)
                    };
                    let card_border = if is_light {
                        Color32::from_rgb(0xDC, 0xDC, 0xDC)
                    } else {
                        Color32::from_rgb(0x33, 0x33, 0x33)
                    };
                    let input_bg = if is_light {
                        Color32::from_rgb(0xF5, 0xF5, 0xF5)
                    } else {
                        Color32::from_rgb(0x18, 0x18, 0x18)
                    };

                    ScrollArea::vertical()
                        .auto_shrink([false, false])
                        .show(ui, |ui| {
                            ui.set_width((ui.available_width() - 12.0).max(320.0));
                            ui.visuals_mut().extreme_bg_color = input_bg;
                            ui.visuals_mut().widgets.inactive.bg_stroke =
                                Stroke::new(1.0_f32, card_border);

                            // Section 1: General (Navigation & Network)
                            Self::section_card(
                                ui,
                                "General",
                                "Startup page, URL formatting, and text encoding",
                                card_bg,
                                card_border,
                                accent,
                                muted_color,
                                |ui| {
                                    Grid::new("settings_general_grid")
                                        .num_columns(2)
                                        .min_col_width(150.0)
                                        .spacing([18.0, 12.0])
                                        .show(ui, |ui| {
                                            Self::label(ui, "Gopher Home", text_color);
                                            ui.add(
                                                TextEdit::singleline(&mut self.draft.gopher_home)
                                                    .desired_width(280.0)
                                                    .margin(Vec2::new(8.0, 6.0)),
                                            );
                                            ui.end_row();

                                            Self::label(ui, "Search Engine", text_color);
                                            ui.add(
                                                TextEdit::singleline(&mut self.draft.search_engine)
                                                    .desired_width(280.0)
                                                    .hint_text("gopher.floodgap.com/v2/vs")
                                                    .margin(Vec2::new(8.0, 6.0)),
                                            );
                                            ui.end_row();

                                            Self::label(ui, "RFC 4266 Type Prefix", text_color);
                                            ui.checkbox(
                                                &mut self.draft.selector_prefix_enabled,
                                                RichText::new("Show /1/, /0/, etc. in address bar")
                                                    .font(FontId::proportional(13.0))
                                                    .color(text_color),
                                            );
                                            ui.end_row();

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
                                },
                            );

                            ui.add_space(16.0);

                            // Section 2: Theme & Typography
                            Self::section_card(
                                ui,
                                "Theme & Typography",
                                "Quick color presets and page text sizing",
                                card_bg,
                                card_border,
                                accent,
                                muted_color,
                                |ui| {
                                    ui.horizontal_wrapped(|ui| {
                                        ui.spacing_mut().button_padding = Vec2::new(12.0, 6.0);
                                        ui.spacing_mut().item_spacing.x = 8.0;

                                        if Self::preset_button(ui, "Default (Dark)") {
                                            self.draft.theme = Theme::default();
                                        }
                                        if Self::preset_button(ui, "Light") {
                                            self.draft.theme = Theme::light();
                                        }
                                        if Self::preset_button(ui, "Grass") {
                                            self.draft.theme = Theme::grass();
                                        }
                                        if Self::preset_button(ui, "Pink") {
                                            self.draft.theme = Theme::pink();
                                        }
                                    });

                                    ui.add_space(14.0);

                                    Grid::new("settings_font_grid")
                                        .num_columns(2)
                                        .min_col_width(150.0)
                                        .spacing([18.0, 12.0])
                                        .show(ui, |ui| {
                                            Self::label(ui, "Window Appearance", text_color);
                                            ui.horizontal(|ui| {
                                                ui.spacing_mut().item_spacing.x = 6.0;
                                                ui.selectable_value(
                                                    &mut self.draft.theme.window_appearance,
                                                    WindowAppearance::Dark,
                                                    "Dark",
                                                );
                                                ui.selectable_value(
                                                    &mut self.draft.theme.window_appearance,
                                                    WindowAppearance::Light,
                                                    "Light",
                                                );
                                                ui.selectable_value(
                                                    &mut self.draft.theme.window_appearance,
                                                    WindowAppearance::System,
                                                    "System",
                                                );
                                            });
                                            ui.end_row();

                                            Self::label(ui, "Address Bar", text_color);
                                            ui.horizontal(|ui| {
                                                ui.spacing_mut().item_spacing.x = 6.0;
                                                ui.selectable_value(
                                                    &mut self.draft.theme.nav_bar_position,
                                                    NavigationBarPosition::Bottom,
                                                    "Bottom",
                                                );
                                                ui.selectable_value(
                                                    &mut self.draft.theme.nav_bar_position,
                                                    NavigationBarPosition::Top,
                                                    "Top",
                                                );
                                            });
                                            ui.end_row();

                                            Self::label(ui, "Page Font Size", text_color);
                                            ui.add(
                                                Slider::new(
                                                    &mut self.draft.theme.page_font_size,
                                                    10.0..=32.0,
                                                )
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

                                            Self::label(ui, "Line Numbers", text_color);
                                            ui.checkbox(
                                                &mut self.draft.theme.show_line_numbers,
                                                RichText::new("Show line numbers in Gopher menus")
                                                    .font(FontId::proportional(13.0))
                                                    .color(text_color),
                                            );
                                            ui.end_row();
                                        });
                                },
                            );

                            ui.add_space(16.0);

                            // Section 3: Page Colors
                            Self::section_card(
                                ui,
                                "Page Colors",
                                "Gophermap and text document palette",
                                card_bg,
                                card_border,
                                accent,
                                muted_color,
                                |ui| {
                                    Grid::new("settings_page_colors")
                                        .num_columns(4)
                                        .spacing([16.0, 12.0])
                                        .show(ui, |ui| {
                                            Self::color_cell(
                                                ui,
                                                "Background",
                                                &mut self.draft.theme.view_background,
                                                text_color,
                                            );
                                            Self::color_cell(
                                                ui,
                                                "Text",
                                                &mut self.draft.theme.view_text,
                                                text_color,
                                            );
                                            ui.end_row();

                                            Self::color_cell(
                                                ui,
                                                "Links",
                                                &mut self.draft.theme.page_link,
                                                text_color,
                                            );
                                            Self::color_cell(
                                                ui,
                                                "Line Numbers",
                                                &mut self.draft.theme.page_line_number,
                                                text_color,
                                            );
                                            ui.end_row();

                                            Self::color_cell(
                                                ui,
                                                "Selection",
                                                &mut self.draft.theme.page_selection,
                                                text_color,
                                            );
                                            ui.end_row();
                                        });
                                },
                            );

                            ui.add_space(16.0);

                            // Section 4: Bars, Banners & Downloads
                            Self::section_card(
                                ui,
                                "Bars, Banners & Downloads",
                                "Toolbar, search bar, alerts, and download list colors",
                                card_bg,
                                card_border,
                                accent,
                                muted_color,
                                |ui| {
                                    Grid::new("settings_chrome_colors")
                                        .num_columns(4)
                                        .spacing([16.0, 12.0])
                                        .show(ui, |ui| {
                                            Self::color_cell(
                                                ui,
                                                "Nav Background",
                                                &mut self.draft.theme.nav_background,
                                                text_color,
                                            );
                                            Self::color_cell(
                                                ui,
                                                "Nav Icons",
                                                &mut self.draft.theme.nav_text,
                                                text_color,
                                            );
                                            ui.end_row();

                                            Self::color_cell(
                                                ui,
                                                "Nav Hover",
                                                &mut self.draft.theme.nav_text_hover,
                                                text_color,
                                            );
                                            Self::color_cell(
                                                ui,
                                                "Address Selection",
                                                &mut self.draft.theme.nav_selection,
                                                text_color,
                                            );
                                            ui.end_row();

                                            Self::color_cell(
                                                ui,
                                                "Search Bar",
                                                &mut self.draft.theme.search_background,
                                                text_color,
                                            );
                                            Self::color_cell(
                                                ui,
                                                "Message Bar",
                                                &mut self.draft.theme.message_background,
                                                text_color,
                                            );
                                            ui.end_row();

                                            Self::color_cell(
                                                ui,
                                                "Message Text",
                                                &mut self.draft.theme.message_foreground,
                                                text_color,
                                            );
                                            Self::color_cell(
                                                ui,
                                                "Downloads Bg",
                                                &mut self.draft.theme.filelist_background,
                                                text_color,
                                            );
                                            ui.end_row();

                                            Self::color_cell(
                                                ui,
                                                "Selected Row",
                                                &mut self.draft.theme.download_selected,
                                                text_color,
                                            );
                                            ui.end_row();
                                        });
                                },
                            );

                            ui.add_space(8.0);
                        });
                });
        });

        if close_requested {
            self.visible = false;
            self.applied_appearance = None;
        }
        saved_settings
    }

    #[allow(clippy::too_many_arguments)]
    fn section_card(
        ui: &mut egui::Ui,
        title: &str,
        subtitle: &str,
        bg: Color32,
        border: Color32,
        title_color: Color32,
        subtitle_color: Color32,
        add_contents: impl FnOnce(&mut egui::Ui),
    ) {
        Frame::new()
            .fill(bg)
            .stroke(Stroke::new(1.0_f32, border))
            .corner_radius(CornerRadius::same(8))
            .inner_margin(Margin::same(16))
            .show(ui, |ui| {
                ui.set_width(ui.available_width());
                ui.label(
                    RichText::new(title)
                        .font(FontId::proportional(15.0))
                        .strong()
                        .color(title_color),
                );
                ui.add_space(2.0);
                ui.label(
                    RichText::new(subtitle)
                        .font(FontId::proportional(12.0))
                        .color(subtitle_color),
                );
                ui.add_space(12.0);
                add_contents(ui);
            });
    }

    fn preset_button(ui: &mut egui::Ui, label: &str) -> bool {
        ui.add(
            Button::new(RichText::new(label).font(FontId::proportional(13.0)))
                .corner_radius(CornerRadius::same(6))
                .min_size(Vec2::new(76.0, 30.0)),
        )
        .on_hover_cursor(CursorIcon::PointingHand)
        .clicked()
    }

    fn label(ui: &mut egui::Ui, text: &str, color: Color32) {
        ui.label(
            RichText::new(text)
                .font(FontId::proportional(13.0))
                .color(color),
        );
    }

    fn color_cell(ui: &mut egui::Ui, label: &str, color: &mut Color32, text_color: Color32) {
        ui.add_sized(
            Vec2::new(124.0, 22.0),
            egui::Label::new(
                RichText::new(label)
                    .font(FontId::proportional(13.0))
                    .color(text_color),
            )
            .wrap_mode(egui::TextWrapMode::Truncate),
        );
        ui.color_edit_button_srgba(color);
    }

    fn bar_button(
        label: &str,
        text_color: Color32,
        border_color: Color32,
        fill: Color32,
    ) -> Button<'_> {
        Button::new(
            RichText::new(label)
                .font(FontId::proportional(13.0))
                .color(text_color),
        )
        .fill(fill)
        .stroke(Stroke::new(1.0_f32, border_color))
        .corner_radius(CornerRadius::same(6))
        .min_size(Vec2::new(88.0, 32.0))
    }
}
