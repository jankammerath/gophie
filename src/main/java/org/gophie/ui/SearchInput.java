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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.gophie.config.*;
import org.gophie.ui.event.SearchInputListener;

public class SearchInput extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;

    public static final String SEARCH_BACKGROUND = "#248AC2";
    public static final String SEARCH_TITLECOLOR = "#76bce3";
    public static final String SEARCH_TEXTCOLOR = "#e8e8e8";

    JLabel searchIcon;
    JLabel searchTitle;
    JTextField searchText;

    public SearchInput(){
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        this.setBorder(new EmptyBorder(6,12,12,14));
        this.setBackground(Color.decode(configFile.getSetting
                ("SEARCH_BACKGROUND", "Appearance", SEARCH_BACKGROUND)));
        
        this.searchIcon = new JLabel("");
        this.searchIcon.setFont(ConfigurationManager.getIconFont(16f));
        this.searchIcon.setBorder(new EmptyBorder(0,0,0,8));
        this.searchIcon.setForeground(Color.decode(configFile.getSetting
                ("SEARCH_TITLECOLOR", "Appearance", SEARCH_TITLECOLOR)));

        this.add(this.searchIcon);

        this.searchTitle = new JLabel("Search");
        this.searchTitle.setForeground(Color.decode(configFile.getSetting
                ("SEARCH_TITLECOLOR", "Appearance", SEARCH_TITLECOLOR)));

        this.searchTitle.setBorder(new EmptyBorder(2,0,0,12));
        this.add(this.searchTitle);

        this.searchText = new JTextField();
        this.searchText.setBorder(new EmptyBorder(2,0,0,0));
        this.searchText.setBackground(Color.decode(configFile.getSetting
                ("SEARCH_BACKGROUND", "Appearance", SEARCH_BACKGROUND)));

        this.searchText.setForeground(Color.decode(configFile.getSetting
                ("SEARCH_TEXTCOLOR", "Appearance", SEARCH_TEXTCOLOR)));

        this.searchText.setCaretColor(Color.decode(configFile.getSetting
                ("SEARCH_TEXTCOLOR", "Appearance", SEARCH_TEXTCOLOR)));

        this.searchText.setFont(ConfigurationManager.getDefaultFont(14f));
        this.add(this.searchText);

        this.setVisible(false);
    }

    public void performSearch(String title, SearchInputListener listener){
        this.searchTitle.setText(title);
        this.searchTitle.setFont(ConfigurationManager.getDefaultFont(14f));
        this.searchText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                /* execute search when the ENTER key is pressed */
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    JTextField textField = (JTextField) e.getSource();

                    /* only execute search when text is not empty */
                    if(textField.getText().length() > 0){
                        listener.searchRequested(textField.getText());
                    }

                    setVisible(false);
                }
                
                /* just cancel when the user hit ESC */
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
                    setVisible(false);
                }
            }
        });
        this.setVisible(true);
        this.searchText.grabFocus();
    }
}