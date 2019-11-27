/*
    This file is part of Gophie.

    Gophie is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gophie is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gophie. If not, see <https://www.gnu.org/licenses/>.

*/

package org.gophie.ui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.gophie.ui.event.PageMenuEventListener;
import org.gophie.ui.util.ImageTransferable;
import org.gophie.net.GopherPage;
import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.net.GopherItem;

public class PageMenu extends PopupMenu {
    private static final long serialVersionUID = 1L;

    /* the menu items */
    private MenuItem saveItem;
    private MenuItem saveTargetItem;
    private MenuItem copyTargetUrl;
    private MenuItem copyTargetText;
    private MenuItem copyImageUrl;
    private MenuItem copyImageObject;
    private MenuItem copySelectedItem;
    private MenuItem copyUrlItem;
    private MenuItem copyTextItem;
    private MenuItem copySourceItem;
    private MenuItem selectAllItem;
    private MenuItem setHomeGopherItem;
    private PopupMenu copyMenu;

    /* private variables */
    private String selectedText = "";
    private GopherItem targetLink;
    private GopherPage currentPage;

    /* list with event listeners to report to */
    private ArrayList<PageMenuEventListener> eventListenerList = new ArrayList<PageMenuEventListener>();

    /**
     * Constructs the page menu
     */
    public PageMenu() {
        super();

        /* request listeners to save the current page as file */
        this.saveItem = new MenuItem("Save Page As...");
        this.saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    for(PageMenuEventListener listener: eventListenerList){
                        listener.pageSaveRequested(currentPage);
                    }
                }
            }       
        });

        /* request listeners to download the file behind the link */
        this.saveTargetItem = new MenuItem("Save Link As...");
        this.saveTargetItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(targetLink != null){
                    for(PageMenuEventListener listener: eventListenerList){
                        listener.itemDownloadRequested(targetLink);
                    }
                }
            }       
        });

        /* copies the url of the link target to the clipboard */
        this.copyTargetUrl = new MenuItem("Copy Link URL");
        this.copyTargetUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(targetLink != null){
                    copyToClipboard(targetLink.getUrlString());
                }
            }       
        });

        /* copies the url of an individually displayed image */
        this.copyImageUrl = new MenuItem("Copy Image URL");
        this.copyImageUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    copyToClipboard(currentPage.getUrl().getUrlString());
                }
            }       
        });

        /* copies the object of the image to clipboard */
        this.copyImageObject = new MenuItem("Copy Image");
        this.copyImageObject.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    copyImageToClipboard();
                }
            }       
        });

        /* copies the text of the active link to the clipboard */
        this.copyTargetText = new MenuItem("Copy Link Text");
        this.copyTargetText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(targetLink != null){
                    copyToClipboard(targetLink.getUserDisplayString());
                }
            }       
        });

        /* copies the currently selected text to the clipboard */
        this.copySelectedItem = new MenuItem("Copy Selection");
        this.copySelectedItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(selectedText.length() > 0){
                    copyToClipboard(selectedText);
                }
            }       
        });

        /* requests event listeners to select all text */
        this.selectAllItem = new MenuItem("Select All");
        this.selectAllItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(PageMenuEventListener listener: eventListenerList){
                    listener.selectAllTextRequested();
                }
            }       
        });

        /* requests listeners to set current page as home page */
        this.setHomeGopherItem = new MenuItem("Set As Home Gopher");
        this.setHomeGopherItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    for(PageMenuEventListener listener: eventListenerList){
                        listener.setHomeGopherRequested(currentPage.getUrl().getUrlString());
                    }
                }
            }       
        });

        /* create the copy menu with its sub-items */
        this.copyMenu = new PopupMenu("Copy");

        /* copies the url of the current page to the clipboard */
        this.copyUrlItem = new MenuItem("URL");
        this.copyUrlItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    copyToClipboard(currentPage.getUrl().getUrlString());
                }
            }       
        });

        /* copies the text of the current page to the clipboard */
        this.copyTextItem = new MenuItem("Page Text");
        this.copyTextItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    copyToClipboard(currentPage.getTextContent());
                }
            }       
        });

        /* copies the source code of the page to the clipboard */
        this.copySourceItem = new MenuItem("Source Code");
        this.copySourceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentPage != null){
                    copyToClipboard(currentPage.getSourceCode());
                }
            }       
        });

        /* add the items to the copy menu */
        this.copyMenu.add(this.copyUrlItem);
        this.copyMenu.add(this.copyTextItem);
        this.copyMenu.add(this.copySourceItem);
    }

    /**
     * Adds and event listener for page menu events
     * 
     * @param listener
     * the listener to add to the list
     */
    public void addPageMenuEventListener(PageMenuEventListener listener){
        this.eventListenerList.add(listener);
    }

    @Override
    public void show(Component origin, int x, int y){
        /* remove all items */
        this.removeAll();

        /* show menu item based on context */
        if(this.targetLink == null){
            /* we do not have a link target */
            Boolean isImage = false;

            /* determine the text for the save item */
            if(currentPage != null){
                switch(currentPage.getContentType()){
                    case GOPHERMENU:
                        /* save gopher menu as */
                        this.saveItem.setLabel("Save Page As ...");
                        break;
                    case IMAGE_FILE:
                        /* save image file as */
                        isImage = true;
                        break;
                    case GIF_FILE:
                        /* save image file as */
                        isImage = true;
                        break;
                    default:
                        /* save file as is the generic label */
                        this.saveItem.setLabel("Save File As ...");
                        break;
                }
            }            

            /* set the proper label for image files */
            if(isImage == true){ this.saveItem.setLabel("Save Image As ..."); }

            this.add(this.saveItem);
            this.addSeparator();

            /* only show selection copy, when selection exists */
            if(this.selectedText.length() > 0 && isImage == false){
                this.add(this.copySelectedItem);
            }

            if(!isImage){ 
                this.add(this.selectAllItem);
                this.add(copyMenu);
                this.addSeparator();
                
                /* only allow setting as home gopher when
                    this page is a gopher menu page */
                if(currentPage.getContentType() == GopherItemType.GOPHERMENU){
                    this.addSeparator();
                    this.add(this.setHomeGopherItem);   
                }   
            }else{
                this.add(this.copyImageObject);
                this.add(this.copyImageUrl);
            }
        }else{
            /* we do have a link target */
            this.add(this.saveTargetItem);
            this.addSeparator();
            this.add(this.copyTargetUrl);

            /* only show selection copy, when selection exists */
            if(this.selectedText.length() > 0){
                this.add(this.copySelectedItem);
            }

            this.add(this.copyTargetText);
        }

        /* call the base method */
        super.show(origin,x,y);
    }

    /**
     * Copies the current image in the page to the clipboard
     */
    private void copyImageToClipboard(){
        if(currentPage != null){
            if(currentPage.getContentType() == GopherItemType.IMAGE_FILE
                || currentPage.getContentType() == GopherItemType.GIF_FILE){
                /* seems to be a valid image file, copy it to clipboard */
                try{
                    InputStream imageInputStream = new ByteArrayInputStream(currentPage.getByteArray());
                    BufferedImage bufferedImage = ImageIO.read(imageInputStream);
                    ImageTransferable transferImage = new ImageTransferable(bufferedImage);
                    Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipBoard.setContents(transferImage, null);
                }catch(Exception ex){
                    /* output information about the clippy failure */
                    System.out.println("Unable to copy image to clipboard: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Copies the text provided to the clipboard
     * 
     * @param text
     * text to copy to the clipboard
     */
    private void copyToClipboard(String text){
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents((new StringSelection(text)), null);
    }

    public void setCurrentPage(GopherPage value){
        /* reset the link target when a new page was loaded */
        this.targetLink = null;

        /* set the current page locally */
        this.currentPage = value;
    }

    public void setLinkTarget(GopherItem value){
        this.targetLink = value;
    }

    public void setSelectedText(String value){
        if(value == null){
            this.selectedText = "";
        }else{
            this.selectedText = value;
        }
    }
}