package org.gophie.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.gophie.config.ConfigurationManager;

public class SearchInput extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;

    public static final String SEARCH_BACKGROUND = "#1b1b1b";
    public static final String SEARCH_TEXTCOLOR = "#e8e8e8";

    JLabel searchIcon;
    JLabel searchTitle;
    JTextField searchText;

    public SearchInput(){
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        this.setBorder(new EmptyBorder(6,12,4,14));
        this.setBackground(Color.decode(SEARCH_BACKGROUND));
        
        this.searchIcon = new JLabel("");
        this.searchIcon.setFont(ConfigurationManager.getIconFont(14f));
        this.searchIcon.setBorder(new EmptyBorder(0,0,0,8));
        this.searchIcon.setForeground(Color.decode(SEARCH_TEXTCOLOR));
        this.add(this.searchIcon);

        this.searchTitle = new JLabel("Search");
        this.searchTitle.setForeground(Color.decode(SEARCH_TEXTCOLOR));
        this.searchTitle.setBorder(new EmptyBorder(2,0,0,8));
        this.add(this.searchTitle);

        this.searchText = new JTextField();
        this.searchText.setBorder(new EmptyBorder(2,0,0,0));
        this.searchText.setBackground(Color.decode(SEARCH_BACKGROUND));
        this.searchText.setForeground(Color.decode(SEARCH_TEXTCOLOR));
        this.searchText.setCaretColor(Color.decode(SEARCH_TEXTCOLOR));
        this.add(this.searchText);

        this.showSearch(false);
    }

    public void showSearch(Boolean visible){
        this.setVisible(visible);
        if(visible == true){
            this.searchText.grabFocus();
        }
    }
}