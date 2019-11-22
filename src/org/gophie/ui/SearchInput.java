package org.gophie.ui;

import javax.swing.*;

public class SearchInput extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;

    JLabel searchIcon;
    JLabel searchTitle;
    JTextField searchText;

    public SearchInput(){
        this.searchIcon = new JLabel("");
        this.searchTitle = new JLabel();
    }
}