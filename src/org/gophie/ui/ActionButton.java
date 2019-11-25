package org.gophie.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.gophie.config.ConfigurationManager;

public class ActionButton extends JPanel {
    private static final long serialVersionUID = 1L;
    
    /* private variables */
    private String inactiveTextColor = "";
    private String textColor = "";
    private Boolean isEnabledButton = false;

    /* private components */
    private JLabel iconLabel;
    private JLabel textLabel;

    public ActionButton(String iconText, String text, String textColorHex, String inactiveTextColorHex){
        /* construct the base */
        super();

        /* set text colors locally */
        this.textColor = textColorHex;
        this.inactiveTextColor = inactiveTextColorHex;

        /* configure the layout for this button */
        this.setLayout(new BorderLayout());
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.setOpaque(false);

        /* icon for the button using the icon font */
        this.iconLabel = new JLabel(iconText);
        this.iconLabel.setBorder(new EmptyBorder(0,0,0,6));
        this.iconLabel.setOpaque(false);
        this.iconLabel.setFont(ConfigurationManager.getIconFont(14f));
        this.iconLabel.setForeground(Color.decode(inactiveTextColorHex));
        this.add(iconLabel,BorderLayout.WEST);

        /* text for the button using the default text font */
        this.textLabel = new JLabel(text);
        this.textLabel.setOpaque(false);
        this.textLabel.setFont(ConfigurationManager.getDefaultFont(12f));
        this.textLabel.setForeground(Color.decode(inactiveTextColorHex));
        this.add(textLabel,BorderLayout.EAST);

        this.addMouseListener(new MouseAdapter() {
            /* notify the listeners of the forward move request */
            public void mouseReleased(MouseEvent evt){
                /* will be handled by another handler */
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                /* only show hover effect when button is enabled */
                if(isButtonEnabled() == true){
                    iconLabel.setForeground(Color.decode(textColor));
                    textLabel.setForeground(Color.decode(textColor));
                }
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                iconLabel.setForeground(Color.decode(inactiveTextColor));
                textLabel.setForeground(Color.decode(inactiveTextColor));
            }
        });
    }

    public void setContent(String iconText, String text){
        this.iconLabel.setText(iconText);
        this.textLabel.setText(text);
    }

    public void setTextColor(String colorHex){
        this.textColor = colorHex;
    }

    /**
     * Sets the color for inactive/disabled button
     * 
     * @param colorHex
     * The color in hex format
     */
    public void setInactiveTextColor(String colorHex){
        this.inactiveTextColor = colorHex;
    }

    /**
     * Enables or disables the button
     * 
     * @param value
     * true means enabled, false is otherwise
     */
    public void setButtonEnabled(Boolean value){
        this.isEnabledButton = value;

        if(value == true){
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }else{
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public Boolean isButtonEnabled(){
        return this.isEnabledButton;
    }
}