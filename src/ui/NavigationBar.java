package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.*;
import java.io.*;

public class NavigationBar extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;

    /* static variables */
    static String textColorHex;
    static String textHoverColorHex;

    /* local variables and objects */
    Font iconFont;
    JButton backButton;
    JButton forwardButton;
    JButton refreshButton;

    /* the constructor essentially builds up the entire
        component with all its sub-components 
        
        @backgroundColor    Background color of this bar
        @textColor          Color of the text and icons    
        @textHoverColor     Color of the text and icons on hover
    */
    public NavigationBar(String backgroundColor, String textColor, String textHoverColor){
        /* get the icon font for this navigation bar */
        try{
            /* try to open the font for icon display */
            this.iconFont = Font.createFont(Font.TRUETYPE_FONT, new File("../resources/Fonts/Feather.ttf")).deriveFont(19f);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(this.iconFont);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the fonts!");
        }

        /* store the text color locally */
        NavigationBar.textHoverColorHex = textHoverColor;
        NavigationBar.textColorHex = textColor;

        /* set the background color initially */
        this.setBackgroundColor(backgroundColor);

        /* adjust the border properly so it looks nice */
        this.setBorder(new EmptyBorder(0,0,0,0));

        this.setLayout(new FlowLayout());

        /* create the buttons */
        this.backButton = this.createButton("");
        this.forwardButton = this.createButton("");
        this.refreshButton = this.createButton("");
    }

    /* 
        Creates a new button on this navigation bar

        @text       text on the new button
        @return     the JButton object created
    */
    private JButton createButton(String text){
        JButton button = new JButton(text);

        /* remove borders and all the button stuff */
        button.setBorderPainted(false); 
        button.setContentAreaFilled(false); 
        button.setFocusPainted(false); 
        button.setOpaque(false);

        /* set the icon font */
        button.setFont(this.iconFont);
        button.setForeground(Color.decode(NavigationBar.textColorHex));
        
        /* add the mouse listeners to create the hover effect */
        button.addMouseListener(new MouseAdapter() {
            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                button.setForeground(Color.decode(NavigationBar.textHoverColorHex));
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                button.setForeground(Color.decode(NavigationBar.textColorHex));
            }
        });

        /* add the button the bar */
        this.add(button);

        return button;
    }

    /* 
        sets the background color has hex (e.g. #ffffff) 
        
        @colorHex       the hex code for the background color
    */
    public void setBackgroundColor(String colorHex){
        this.setBackground(Color.decode(colorHex));
    }
}