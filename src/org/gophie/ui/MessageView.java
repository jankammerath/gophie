package org.gophie.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.border.*;

import org.gophie.config.ConfigurationManager;
import org.gophie.ui.event.MessageViewListener;

public class MessageView extends JPanel {
    /* constants */
    private static final long serialVersionUID = 1L;
    private static final String BACKGROUND_COLOR = "#fcba03";

    /* local components */
    private JLabel messageIcon;
    private JLabel messageText;
    private JPanel buttonPanel;
    private Font iconFont;

    /**
     * Constructs the message view and does not show it by default as one of the
     * show-methods should be called
     */
    public MessageView() {
        /* get the icon font for this navigation bar */
        this.iconFont = ConfigurationManager.getIconFont(19f);

        /* set box layout for this message view */
        this.setLayout(new BorderLayout());
        this.setBorder(new EmptyBorder(10, 10, 10, 16));
        this.setBackground(Color.decode(BACKGROUND_COLOR));

        /* create the label instance */
        this.messageIcon = new JLabel();
        this.messageIcon.setFont(this.iconFont);
        this.messageIcon.setBorder(new EmptyBorder(0, 5, 0, 10));
        this.messageText = new JLabel();
        this.buttonPanel = new JPanel();
        this.buttonPanel.setBorder(new EmptyBorder(0, 30, 0, 0));
        this.buttonPanel.setLayout(new BoxLayout(this.buttonPanel, BoxLayout.X_AXIS));
        this.buttonPanel.setBackground(Color.decode(BACKGROUND_COLOR));
        this.add(this.messageIcon, BorderLayout.WEST);
        this.add(this.messageText, BorderLayout.CENTER);
        this.add(this.buttonPanel, BorderLayout.EAST);

        /* do not show by default */
        this.setVisible(false);
    }

    /**
     * Creates a new button for this view
     * 
     * @param text
     * The text on the button as string
     * 
     * @return
     * The button in the form of a JLabel
     */
    private JLabel createButton(String text) {
        JLabel customButton = new JLabel("<html><div style=\"border:1px solid #000000;"
                + "padding:2px 6px 2px 6px;border-radius:6px;\">" + text + "</div></html>");
        customButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        customButton.setBorder(new EmptyBorder(0, 5, 0, 5));
        return customButton;
    }

    /**
     * Shows an information message
     * 
     * @param text
     * Text of the information message as string
     */
    public void showInfo(String text) {
        /* build up icon and text */
        this.messageIcon.setText("");
        this.messageText.setText(text);
        this.buttonPanel.removeAll();

        /* add the handle to close this message */
        JLabel closeButton = this.createButton("Close");
        closeButton.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt){
                setVisible(false);
            }
        });

        /* create and set to visible */
        this.buttonPanel.add(closeButton);
        this.setVisible(true);
    }

    /**
     * Shows a confirm message with options
     * 
     * @param text
     * The text to show in the message view
     * 
     * @param optionList
     * A string array with the options
     */
    public void showConfirm(String text, String[] optionList, MessageViewListener eventListener){
        /* remove all components */
        this.messageIcon.setText("");
        this.messageText.setText(text);

        /* build the option buttons */
        this.buttonPanel.removeAll();
        for(int i=0; i<optionList.length; i++){
            JLabel optionButton = this.createButton(optionList[i]);
            final int optionId = i;
            optionButton.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent evt){
                    eventListener.optionSelected(optionId);
                }
            });

            this.buttonPanel.add(optionButton);
        }

        this.setVisible(true);
    }
}