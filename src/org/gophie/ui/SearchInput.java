package org.gophie.ui;

import javax.swing.*;

import org.gophie.config.ConfigurationManager;

public class SearchInput extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;

    JLabel searchIcon;
    JLabel searchTitle;
    JTextField searchText;

    public SearchInput(){
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        
        this.searchIcon = new JLabel("");
        this.searchIcon.setFont(ConfigurationManager.getIconFont(19f));
        this.add(this.searchIcon);

        this.searchTitle = new JLabel("Search");
        this.add(this.searchTitle);

        this.searchText = new JTextField();
        this.add(this.searchText);
    }
}