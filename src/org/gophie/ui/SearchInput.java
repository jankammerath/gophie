package org.gophie.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.gophie.config.ConfigurationManager;
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
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        this.setBorder(new EmptyBorder(6,12,12,14));
        this.setBackground(Color.decode(SEARCH_BACKGROUND));
        
        this.searchIcon = new JLabel("");
        this.searchIcon.setFont(ConfigurationManager.getIconFont(14f));
        this.searchIcon.setBorder(new EmptyBorder(0,0,0,8));
        this.searchIcon.setForeground(Color.decode(SEARCH_TITLECOLOR));
        this.add(this.searchIcon);

        this.searchTitle = new JLabel("Search");
        this.searchTitle.setForeground(Color.decode(SEARCH_TITLECOLOR));
        this.searchTitle.setBorder(new EmptyBorder(2,0,0,12));
        this.add(this.searchTitle);

        this.searchText = new JTextField();
        this.searchText.setBorder(new EmptyBorder(2,0,0,0));
        this.searchText.setBackground(Color.decode(SEARCH_BACKGROUND));
        this.searchText.setForeground(Color.decode(SEARCH_TEXTCOLOR));
        this.searchText.setCaretColor(Color.decode(SEARCH_TEXTCOLOR));
        this.searchText.setFont(ConfigurationManager.getDefaultFont(13f));
        this.add(this.searchText);

        this.setVisible(false);
    }

    public void performSearch(String title, SearchInputListener listener){
        this.searchTitle.setText(title);
        this.searchTitle.setFont(ConfigurationManager.getDefaultFont(13f));
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