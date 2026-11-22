//! High-performance virtualized Gopher page viewer for menus, plain text, and inline images.

use eframe::egui::{
    self, Align, ColorImage, CursorIcon, FontId, Frame, Layout, Margin, RichText, ScrollArea,
    Sense, TextureHandle, TextureOptions, Vec2,
};

use crate::config::Theme;
use crate::net::{GopherItem, GopherItemType, GopherPage};

use super::assets::{feather_font, icons};
use super::page_menu::{PageMenu, PageMenuAction};

/// User interactions emitted by `PageView`.
#[derive(Debug, Clone)]
pub enum PageViewAction {
    LinkClicked(GopherItem),
    Menu(PageMenuAction),
}

struct RenderContext<'a> {
    theme: &'a Theme,
    selected_all: bool,
    selected_text: &'a str,
    selector_prefix_enabled: bool,
    scroll_to_top: bool,
}

enum CachedContent {
    Empty,
    Menu,
    Text { lines: Vec<String> },
    Image { texture: TextureHandle, size: Vec2 },
    ImageError(String),
}

/// Renders the active `GopherPage` with virtualized row scrolling and cached image textures.
pub struct PageView {
    current_page: Option<GopherPage>,
    cached: CachedContent,
    selected_all: bool,
    scroll_to_top: bool,
}

impl Default for PageView {
    fn default() -> Self {
        Self::new()
    }
}

impl PageView {
    pub fn new() -> Self {
        Self {
            current_page: None,
            cached: CachedContent::Empty,
            selected_all: false,
            scroll_to_top: false,
        }
    }

    /// Returns the currently displayed `GopherPage`, if any.
    pub fn current_page(&self) -> Option<&GopherPage> {
        self.current_page.as_ref()
    }

    /// Loads a new `GopherPage`, caching decoded lines or GPU image textures once.
    pub fn set_page(&mut self, ctx: &egui::Context, page: GopherPage) {
        self.selected_all = false;
        self.scroll_to_top = true;

        self.cached = match page.content_type() {
            GopherItemType::GopherMenu => CachedContent::Menu,
            GopherItemType::ImageFile | GopherItemType::GifFile => {
                Self::decode_image_texture(ctx, &page)
            }
            _ => {
                let cleaned = page.text_content();
                let lines = cleaned.lines().map(str::to_owned).collect();
                CachedContent::Text { lines }
            }
        };

        self.current_page = Some(page);
    }

    /// Marks all text on the current page as selected for copying.
    pub fn select_all(&mut self) {
        if self
            .current_page
            .as_ref()
            .is_some_and(|p| !p.content_type().is_image())
        {
            self.selected_all = true;
        }
    }

    /// Clears any active selection highlight.
    pub fn clear_selection(&mut self) {
        self.selected_all = false;
    }

    /// Returns the currently selected text (full page text when Select All is active).
    pub fn selected_text(&self) -> String {
        if self.selected_all {
            self.current_page
                .as_ref()
                .map(GopherPage::text_content)
                .unwrap_or_default()
        } else {
            String::new()
        }
    }

    /// Renders the page view and returns any link click or context-menu action.
    pub fn show(
        &mut self,
        ui: &mut egui::Ui,
        theme: &Theme,
        selector_prefix_enabled: bool,
    ) -> Option<PageViewAction> {
        let mut action = None;

        Frame::new()
            .fill(theme.view_background)
            .inner_margin(Margin {
                left: 12,
                right: 0,
                top: 8,
                bottom: 8,
            })
            .show(ui, |ui| {
                ui.set_min_size(ui.available_size());
                // Use a predictable desktop-style solid scrollbar flush to the right edge.
                ui.style_mut().spacing.scroll = egui::style::ScrollStyle::solid();

                let selected_text = self.selected_text();
                let render_ctx = RenderContext {
                    theme,
                    selected_all: self.selected_all,
                    selected_text: &selected_text,
                    selector_prefix_enabled,
                    scroll_to_top: std::mem::take(&mut self.scroll_to_top),
                };

                let Some(page) = &self.current_page else {
                    return;
                };

                match &self.cached {
                    CachedContent::Empty => {}
                    CachedContent::Menu => {
                        if let Some(a) = Self::render_menu(ui, page, &render_ctx) {
                            action = Some(a);
                        }
                    }
                    CachedContent::Text { lines } => {
                        if let Some(a) = Self::render_text(ui, page, lines, &render_ctx) {
                            action = Some(a);
                        }
                    }
                    CachedContent::Image { texture, size } => {
                        let mut scroll = ScrollArea::both()
                            .id_salt("gophie_image_scroll")
                            .auto_shrink([false, false]);
                        if render_ctx.scroll_to_top {
                            scroll = scroll.vertical_scroll_offset(0.0);
                        }
                        scroll.show(ui, |ui| {
                            if let Some(a) = Self::render_image(
                                ui,
                                page,
                                texture,
                                *size,
                                selector_prefix_enabled,
                            ) {
                                action = Some(a);
                            }
                        });
                    }
                    CachedContent::ImageError(msg) => {
                        ui.label(
                            RichText::new(format!("Failed to display image:\n{}", msg))
                                .font(FontId::monospace(theme.page_font_size))
                                .color(theme.view_text),
                        );
                    }
                }
            });

        action
    }

    fn render_menu(
        ui: &mut egui::Ui,
        page: &GopherPage,
        rc: &RenderContext<'_>,
    ) -> Option<PageViewAction> {
        let mut action = None;
        let theme = rc.theme;
        let items = page.items();
        let row_height = theme.page_font_size + 5.0;
        let digit_count = items.len().max(1).to_string().len().max(2);
        let line_num_width = (digit_count as f32) * (theme.page_font_size * 0.56) + 8.0;
        let icon_col_width = theme.page_icon_font_size + 12.0;

        // Must be set before `show_rows` so egui's virtualized height matches rendered rows exactly.
        ui.spacing_mut().item_spacing.y = 0.0;

        let mut scroll = ScrollArea::vertical()
            .id_salt("gophie_menu_rows")
            .auto_shrink([false, false]);
        if rc.scroll_to_top {
            scroll = scroll.vertical_scroll_offset(0.0);
        }

        scroll.show_rows(ui, row_height, items.len(), |ui, row_range| {
            ui.spacing_mut().item_spacing.y = 0.0;

                for idx in row_range {
                    let item = &items[idx];
                    let line_number = idx + 1;

                    ui.horizontal(|ui| {
                        ui.set_height(row_height);
                        ui.spacing_mut().item_spacing.x = 4.0;

                        // Line number gutter
                        ui.allocate_ui_with_layout(
                            Vec2::new(line_num_width, row_height),
                            Layout::right_to_left(Align::Center),
                            |ui| {
                                ui.label(
                                    RichText::new(line_number.to_string())
                                        .font(FontId::monospace(theme.page_font_size))
                                        .color(theme.page_line_number),
                                );
                            },
                        );

                        // Feather item-type icon column
                        ui.allocate_ui_with_layout(
                            Vec2::new(icon_col_width, row_height),
                            Layout::centered_and_justified(egui::Direction::LeftToRight),
                            |ui| {
                                let glyph = icons::for_item_type(item.item_type());
                                if !glyph.is_empty() {
                                    ui.label(
                                        RichText::new(glyph)
                                            .font(feather_font(theme.page_icon_font_size))
                                            .color(theme.view_text),
                                    );
                                }
                            },
                        );

                        // Item text or clickable link
                        let display = if item.display_text().is_empty() {
                            " "
                        } else {
                            item.display_text()
                        };

                        if item.is_link() {
                            let mut text = RichText::new(display)
                                .font(FontId::monospace(theme.page_font_size))
                                .color(theme.page_link);
                            if rc.selected_all {
                                text = text.background_color(theme.page_selection);
                            }

                            let resp = ui
                                .add(egui::Label::new(text).sense(Sense::click()).extend())
                                .on_hover_cursor(CursorIcon::PointingHand);

                            if resp.hovered() {
                                let rect = resp.rect;
                                ui.painter().line_segment(
                                    [
                                        egui::pos2(rect.left(), rect.bottom() - 1.0),
                                        egui::pos2(rect.right(), rect.bottom() - 1.0),
                                    ],
                                    egui::Stroke::new(1.0_f32, theme.page_link),
                                );
                            }

                            if resp.clicked() {
                                action = Some(PageViewAction::LinkClicked(item.clone()));
                            }

                            resp.context_menu(|ui| {
                                if let Some(menu_act) = PageMenu::show_link_menu(
                                    ui,
                                    item,
                                    rc.selected_text,
                                    rc.selector_prefix_enabled,
                                ) {
                                    action = Some(PageViewAction::Menu(menu_act));
                                }
                            });
                        } else {
                            let mut text = RichText::new(display)
                                .font(FontId::monospace(theme.page_font_size))
                                .color(theme.view_text);
                            if rc.selected_all {
                                text = text.background_color(theme.page_selection);
                            }

                            let resp = ui.add(
                                egui::Label::new(text)
                                    .sense(Sense::click())
                                    .extend(),
                            );
                            resp.context_menu(|ui| {
                                if let Some(menu_act) = PageMenu::show_page_menu(
                                    ui,
                                    page,
                                    rc.selected_text,
                                    rc.selector_prefix_enabled,
                                ) {
                                    action = Some(PageViewAction::Menu(menu_act));
                                }
                            });
                        }
                    });
                }
            });

        action
    }

    fn render_text(
        ui: &mut egui::Ui,
        page: &GopherPage,
        lines: &[String],
        rc: &RenderContext<'_>,
    ) -> Option<PageViewAction> {
        let mut action = None;
        let theme = rc.theme;
        let row_height = theme.page_font_size + 4.0;

        // Must be set before `show_rows` so egui's virtualized height matches rendered rows exactly.
        ui.spacing_mut().item_spacing.y = 0.0;

        let mut scroll = ScrollArea::vertical()
            .id_salt("gophie_text_rows")
            .auto_shrink([false, false]);
        if rc.scroll_to_top {
            scroll = scroll.vertical_scroll_offset(0.0);
        }

        scroll.show_rows(ui, row_height, lines.len().max(1), |ui, row_range| {
            ui.spacing_mut().item_spacing.y = 0.0;
            for idx in row_range {
                let line = lines.get(idx).map(String::as_str).unwrap_or("");
                let display = if line.is_empty() { " " } else { line };
                let mut text = RichText::new(display)
                    .font(FontId::monospace(theme.page_font_size))
                    .color(theme.view_text);
                if rc.selected_all {
                    text = text.background_color(theme.page_selection);
                }

                ui.horizontal(|ui| {
                    ui.set_height(row_height);
                    let resp = ui.add(egui::Label::new(text).sense(Sense::click()).extend());
                    resp.context_menu(|ui| {
                        if let Some(menu_act) = PageMenu::show_page_menu(
                            ui,
                            page,
                            rc.selected_text,
                            rc.selector_prefix_enabled,
                        ) {
                            action = Some(PageViewAction::Menu(menu_act));
                        }
                    });
                });
            }
        });

        action
    }

    fn render_image(
        ui: &mut egui::Ui,
        page: &GopherPage,
        texture: &TextureHandle,
        original_size: Vec2,
        selector_prefix_enabled: bool,
    ) -> Option<PageViewAction> {
        let mut action = None;
        let max_width = ui.available_width().clamp(100.0, 800.0);
        let display_size = if original_size.x > max_width {
            let scale = max_width / original_size.x;
            Vec2::new(max_width, original_size.y * scale)
        } else {
            original_size
        };

        let resp = ui.add(
            egui::Image::new((texture.id(), display_size)).sense(Sense::click()),
        );
        resp.context_menu(|ui| {
            if let Some(menu_act) =
                PageMenu::show_page_menu(ui, page, "", selector_prefix_enabled)
            {
                action = Some(PageViewAction::Menu(menu_act));
            }
        });

        action
    }

    fn decode_image_texture(ctx: &egui::Context, page: &GopherPage) -> CachedContent {
        match image::load_from_memory(page.raw_bytes()) {
            Ok(img) => {
                let rgba = img.into_rgba8();
                let (w, h) = rgba.dimensions();
                let color_image = ColorImage::from_rgba_unmultiplied(
                    [w as usize, h as usize],
                    rgba.as_raw(),
                );
                let texture = ctx.load_texture(
                    format!("gophie_img_{}", page.url().to_url_string(false)),
                    color_image,
                    TextureOptions::LINEAR,
                );
                CachedContent::Image {
                    texture,
                    size: Vec2::new(w as f32, h as f32),
                }
            }
            Err(err) => CachedContent::ImageError(err.to_string()),
        }
    }
}
