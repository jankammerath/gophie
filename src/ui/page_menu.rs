//! Context menu for Gopher pages, links, and inline images, plus native clipboard helpers.

use eframe::egui;

use crate::net::{GopherItem, GopherItemType, GopherPage, GopherUrl};

/// Actions triggered from the right-click page or link context menu.
#[derive(Debug, Clone)]
pub enum PageMenuAction {
    SaveCurrentPage,
    SaveLinkAs(GopherItem),
    SelectAll,
    SetAsHomeGopher(String),
}

/// Builds context menus and performs cross-platform text and image clipboard copies.
pub struct PageMenu;

impl PageMenu {
    /// Renders the context menu for a hovered/clicked Gopher link item.
    pub fn show_link_menu(
        ui: &mut egui::Ui,
        item: &GopherItem,
        selected_text: &str,
        selector_prefix_enabled: bool,
    ) -> Option<PageMenuAction> {
        let mut action = None;

        if ui.button("Save Link As...").clicked() {
            action = Some(PageMenuAction::SaveLinkAs(item.clone()));
            ui.close_menu();
        }
        ui.separator();

        if ui.button("Copy Link URL").clicked() {
            let mut target_url = item.url_string();
            if selector_prefix_enabled && target_url.starts_with("gopher://") {
                let mut parsed = GopherUrl::parse(&target_url);
                parsed.set_type_prefix(item.item_type_code());
                target_url = parsed.to_url_string(true);
            }
            Self::copy_text(&target_url);
            ui.close_menu();
        }

        if !selected_text.is_empty() && ui.button("Copy Selection").clicked() {
            Self::copy_text(selected_text);
            ui.close_menu();
        }

        if ui.button("Copy Link Text").clicked() {
            Self::copy_text(item.display_text());
            ui.close_menu();
        }

        action
    }

    /// Renders the context menu for the currently displayed Gopher page (menu, text, or image).
    pub fn show_page_menu(
        ui: &mut egui::Ui,
        page: &GopherPage,
        selected_text: &str,
        selector_prefix_enabled: bool,
    ) -> Option<PageMenuAction> {
        let mut action = None;
        let is_image = page.content_type().is_image();

        let save_label = match page.content_type() {
            GopherItemType::GopherMenu => "Save Page As...",
            GopherItemType::ImageFile | GopherItemType::GifFile => "Save Image As...",
            _ => "Save File As...",
        };

        if ui.button(save_label).clicked() {
            action = Some(PageMenuAction::SaveCurrentPage);
            ui.close_menu();
        }
        ui.separator();

        if is_image {
            if ui.button("Copy Image").clicked() {
                Self::copy_image_bytes(page.raw_bytes());
                ui.close_menu();
            }
            if ui.button("Copy Image URL").clicked() {
                let url_str = if selector_prefix_enabled {
                    let mut parsed = page.url().clone();
                    parsed.set_type_prefix(page.content_type().code());
                    parsed.to_url_string(true)
                } else {
                    page.url().to_url_string(false)
                };
                Self::copy_text(&url_str);
                ui.close_menu();
            }
        } else {
            if !selected_text.is_empty() && ui.button("Copy Selection").clicked() {
                Self::copy_text(selected_text);
                ui.close_menu();
            }
            if ui.button("Select All").clicked() {
                action = Some(PageMenuAction::SelectAll);
                ui.close_menu();
            }

            ui.menu_button("Copy", |ui| {
                if ui.button("URL").clicked() {
                    Self::copy_text(&page.url().to_url_string(false));
                    ui.close_menu();
                }
                if ui.button("Page Text").clicked() {
                    Self::copy_text(&page.text_content());
                    ui.close_menu();
                }
                if ui.button("Source Code").clicked() {
                    Self::copy_text(page.source_code());
                    ui.close_menu();
                }
            });

            if page.content_type() == GopherItemType::GopherMenu {
                ui.separator();
                if ui.button("Set As Home Gopher").clicked() {
                    action = Some(PageMenuAction::SetAsHomeGopher(
                        page.url().to_url_string(false),
                    ));
                    ui.close_menu();
                }
            }
        }

        action
    }

    /// Copies plain text to the native system clipboard.
    pub fn copy_text(text: &str) {
        if let Ok(mut clipboard) = arboard::Clipboard::new() {
            let _ = clipboard.set_text(text.to_owned());
        }
    }

    /// Decodes raw image bytes and copies the RGBA bitmap to the native system clipboard.
    pub fn copy_image_bytes(bytes: &[u8]) {
        let Ok(img) = image::load_from_memory(bytes) else {
            return;
        };
        let rgba = img.into_rgba8();
        let (width, height) = rgba.dimensions();
        if let Ok(mut clipboard) = arboard::Clipboard::new() {
            let _ = clipboard.set_image(arboard::ImageData {
                width: width as usize,
                height: height as usize,
                bytes: std::borrow::Cow::Owned(rgba.into_raw()),
            });
        }
    }
}
