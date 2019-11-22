package org.gophie.ui;

import javax.swing.*;

public class SearchInput extends JPanel {
    JLabel searchIcon;
    JLabel searchTitle;
    JTextField searchText;

    public SearchInput(){
        this.searchIcon = new JLabel("");
        this.searchTitle = new JLabel();
    }
}