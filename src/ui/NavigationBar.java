package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

import net.GopherItem.GopherItemType;
import ui.event.NavigationInputListener;

import javax.swing.*;
import java.io.*;
import java.util.*;

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
    private JTextField addressInput;
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

        /* create the address input */
        this.addressInput = this.createAddressInput();
        this.addressInput.setSelectionColor(Color.decode(this.selectionColor));
        this.addressInput.setCaretColor(Color.decode(NavigationBar.textColorHex));

        /* create the refresh button and handle it */
        this.refreshButton = this.createButton("");
        this.refreshButton.addMouseListener(new MouseAdapter() {
            /* initiate a refresh or cancel process */
            public void mouseReleased(MouseEvent evt){
                /* request a stop or cancellation when currently loading */
                if(isLoadingStatus == true){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.stopRequested();
                    }
                }

                /* request a refresh when currently in network idle */
                if(isLoadingStatus == false){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.refreshRequested();
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
            this.refreshButton.setText("");
        }else{
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
        inputField.setBorder(new EmptyBorder(4,16,6,4));
        inputField.setForeground(Color.decode(NavigationBar.textColorHex));
        inputField.setOpaque(false);
        this.add(inputField);

        inputField.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String requestedAddress = inputField.getText().trim();
                if(requestedAddress.length() > 0){
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.addressRequested(requestedAddress, GopherItemType.UNKNOWN);
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