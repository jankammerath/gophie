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
import javax.swing.border.*;
import javax.swing.*;
import java.util.*;

import org.gophie.config.*;
import org.gophie.net.GopherItem;
import org.gophie.ui.event.NavigationInputListener;

public class NavigationBar extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;

    /* static variables */
    static String textColorHex;
    static String textHoverColorHex;
    private String selectionColor = "#ffffff";

    /* local variables and objects */
    private Font iconFont;
    private JLabel backButton;
    private JLabel forwardButton;
    private JLabel refreshButton;
    private JLabel homeButton;
    private JTextField addressInput;
    private JLabel downloadButton;
    private JLabel statusIcon;
    private Boolean isLoadingStatus = false;
    private Boolean allowNavigateForward = false;
    private Boolean allowNavigateBack = false;

    /* listeners for local events */
    private ArrayList<NavigationInputListener> inputListenerList;

    /* the constructor essentially builds up the entire
        component with all its sub-components 
        
        @backgroundColor    Background color of this bar
        @textColor          Color of the text and icons    
        @textHoverColor     Color of the text and icons on hover
    */
    public NavigationBar(String backgroundColor, String textColor, String textHoverColor){
        this.inputListenerList = new ArrayList<NavigationInputListener>();

        /* get the config file for color scheme */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        /* get the icon font from the configuration */
        this.iconFont = ConfigurationManager.getIconFont(19f);

        /* store the text color locally */
        NavigationBar.textHoverColorHex = textHoverColor;
        NavigationBar.textColorHex = textColor;

        /* set the background color initially */
        this.setBackgroundColor(backgroundColor);

        /* set the layout for this navigation bar */
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        this.setBorder(new EmptyBorder(4,4,4,4));

        /* create the back navigation button */
        this.backButton = this.createButton("");
        this.backButton.addMouseListener(new MouseAdapter() {
            /* notify the listeners of the move back request */
            public void mouseReleased(MouseEvent evt){
                if(allowNavigateBack == true){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.backwardRequested();
                    }
                }
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                if(allowNavigateBack == true){
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    backButton.setForeground(Color.decode(NavigationBar.textHoverColorHex));
                }
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                backButton.setForeground(Color.decode(NavigationBar.textColorHex));
            }
        });

        /* create the forward navigation button */
        this.forwardButton = this.createButton("");
        this.forwardButton.addMouseListener(new MouseAdapter() {
            /* notify the listeners of the forward move request */
            public void mouseReleased(MouseEvent evt){
                if(allowNavigateForward == true){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.forwardRequested();
                    }
                }
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                if(allowNavigateForward == true){
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    forwardButton.setForeground(Color.decode(NavigationBar.textHoverColorHex));
                }
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                forwardButton.setForeground(Color.decode(NavigationBar.textColorHex));
            }
        });

        /* create the refresh button and handle it */
        this.refreshButton = this.createButton("");
        this.refreshButton.addMouseListener(new MouseAdapter() {
            /* initiate a refresh or cancel process */
            public void mouseReleased(MouseEvent evt){
                /* request a refresh when currently in network idle */
                if(isLoadingStatus == false){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.refreshRequested();
                    }
                }

                /* request a stop or cancellation when currently loading */
                if(isLoadingStatus == true){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.stopRequested();
                    }
                }
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                refreshButton.setForeground(Color.decode(NavigationBar.textHoverColorHex));
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                refreshButton.setForeground(Color.decode(NavigationBar.textColorHex));
            }
        });

        /* create the refresh button and handle it */
        this.homeButton = this.createButton("");
        this.homeButton.addMouseListener(new MouseAdapter() {
            /* request navigation to the home page */
            public void mouseReleased(MouseEvent evt){
                /* request to g to home gopher page */
                for (NavigationInputListener inputListener : inputListenerList){
                    inputListener.homeGopherRequested();
                }                
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                homeButton.setForeground(Color.decode(NavigationBar.textHoverColorHex));
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                homeButton.setForeground(Color.decode(NavigationBar.textColorHex));
            }
        });

        /* create the address input */
        this.addressInput = this.createAddressInput();
        this.addressInput.setSelectionColor(Color.decode(configFile.getSetting
            ("NAVIGATIONBAR_SELECTION_COLOR", "Appearance", this.selectionColor)));
        this.addressInput.setCaretColor(Color.decode(NavigationBar.textColorHex));

        /* create the download button and handle it */
        this.downloadButton = this.createButton("");
        this.downloadButton.addMouseListener(new MouseAdapter() {
            /* notify the listeners of the forward move request */
            public void mouseReleased(MouseEvent evt){
                for (NavigationInputListener inputListener : inputListenerList){
                    inputListener.showDownloadRequested();
                }
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                downloadButton.setForeground(Color.decode(NavigationBar.textHoverColorHex));
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                downloadButton.setForeground(Color.decode(NavigationBar.textColorHex));
            }
        });

        /* create the status indicator */
        ImageIcon statusIconImage = ConfigurationManager.getImageIcon("loading.gif");
        this.statusIcon = new JLabel(statusIconImage);
        this.statusIcon.setBorder(new EmptyBorder(0,8,0,2));
        this.statusIcon.setOpaque(false);
        this.statusIcon.setVisible(false);
        this.add(this.statusIcon);
    }

    /**
     * Defines whether forward navigation is enabled
     * 
     * @param value
     * true enables navigation, false disables
     */
    public void setNavigateForward(Boolean value){
        this.allowNavigateForward = value;
    }

    /**
     * Defines whether navigation back is enabled
     * 
     * @param value
     * true enables navigation, false disables
     */
    public void setNavigateBack(Boolean value){
        this.allowNavigateBack = value;
    }

    /**
     * Defines whether is loading content or not
     * 
     * @param status
     * true when currently loading content, false if not
     */
    public void setIsLoading(Boolean status){
        this.isLoadingStatus = status;

        if(status == true){
            this.statusIcon.setVisible(true);
            this.refreshButton.setText("");
        }else{
            this.statusIcon.setVisible(false);
            this.refreshButton.setText("");
        }
    }

    /* 
        Adds a listener for navigation events
    */
    public void addListener(NavigationInputListener listener){
        this.inputListenerList.add(listener);
    }

    /*
        Sets the text for the address input

        @addressText    text to insert into address input
    */
    public void setAddressText(String addressText){
        this.addressInput.setText(addressText);
    }

    /*
        Creates the address input to insert gopher URLs in

        @return     the JTextField which is the address input
    */
    private JTextField createAddressInput(){
        JTextField inputField = new JTextField();
        inputField.setBorder(new EmptyBorder(4,10,6,4));
        inputField.setFont(ConfigurationManager.getDefaultFont(14f));
        inputField.setForeground(Color.decode(NavigationBar.textColorHex));
        inputField.setOpaque(false);
        this.add(inputField);

        inputField.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String requestedAddress = inputField.getText().trim();
                if(requestedAddress.length() > 0){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.addressRequested(requestedAddress, new GopherItem());
                    }
                }
            }
        });

        return inputField;
    }

    /* 
        Creates a new button on this navigation bar

        @text       text on the new button
        @return     the JButton object created
    */
    private JLabel createButton(String text){
        JLabel button = new JLabel(text);

        /* set the icon font */
        button.setFont(this.iconFont);
        button.setForeground(Color.decode(NavigationBar.textColorHex));
        
        /* set the border properly to give some space */
        button.setBorder(new EmptyBorder(0,8,0,8));

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