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

import org.gophie.net.GopherItem;

public class PageMenu extends PopupMenu {
    private static final long serialVersionUID = 1L;

    /* the menu items */
    private MenuItem saveItem;
    private MenuItem saveTargetItem;
    private MenuItem copyTargetUrl;
    private MenuItem copyTargetText;
    private MenuItem copySelectedItem;
    private MenuItem copyUrlItem;
    private MenuItem copyTextItem;
    private MenuItem copySourceItem;
    private MenuItem selectAllItem;
    private MenuItem setHomeGopherItem;
    private PopupMenu copyMenu;

    /* private variables */
    private String selectedText = "";
    private GopherItem targetLink = null;

    /**
     * Constructs the page menu
     */
    public PageMenu(){
        super();

        /* instanciate the menu items */
        this.saveItem = new MenuItem("Save Page As...");
        this.saveTargetItem = new MenuItem("Save Link As...");
        this.copyTargetUrl = new MenuItem("Copy Link URL");
        this.copyTargetText = new MenuItem("Copy Link Text");
        this.copySelectedItem = new MenuItem("Copy Selection");
        this.selectAllItem = new MenuItem("Select All");
        this.setHomeGopherItem = new MenuItem("Set As Home Gopher");

        /* create the copy menu with its sub-items */
        this.copyMenu = new PopupMenu("Copy");
        this.copyUrlItem = new MenuItem("URL");
        this.copyTextItem = new MenuItem("Page Text");
        this.copySourceItem = new MenuItem("Source Code");
        this.copyMenu.add(this.copyUrlItem);
        this.copyMenu.add(this.copyTextItem);
        this.copyMenu.add(this.copySourceItem);
    }

    @Override
    public void show(Component origin, int x, int y){
        /* remove all items */
        this.removeAll();

        /* show menu item based on context */
        if(this.targetLink == null){
            /* we do not have a link target */
            this.add(this.saveItem);
            this.addSeparator();

            /* only show selection copy, when selection exists */
            if(this.selectedText.length() > 0){
                this.add(this.copySelectedItem);
            }
            
            this.add(this.selectAllItem);
            this.add(copyMenu);
            this.addSeparator();
            this.add(this.setHomeGopherItem);            
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
            this.addSeparator();
            this.add(this.setHomeGopherItem);      
        }

        /* call the base method */
        super.show(origin,x,y);
    }

    public void setLinkTarget(GopherItem value){
        this.targetLink = value;
    }

    public void setSelectedText(String value){
        this.selectedText = "";
    }
}