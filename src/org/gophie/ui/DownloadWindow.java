package org.gophie.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.gophie.config.ConfigurationManager;
import org.gophie.net.*;
import org.gophie.net.event.*;

public class DownloadWindow{
    private static final String ACTIONBAR_BACKGROUND = "#248AC2";
    private static final String ACTIONBAR_TEXTCOLOR = "#ffffff";
    private static final String ACTIONBAR_INACTIVE_TEXTCOLOR = "#76bce3";
    private static final String FILELIST_BACKGROUND = "#1b1b1b";

    /* local objects */
    private DownloadList list;
    private DownloadItem[] data;

    /* local components */
    private JDialog frame;
    private JList<DownloadItem> fileListView;
    private JPanel actionBar = new JPanel();
    private JPanel clearButton;
    private JPanel actionButton;

    public DownloadWindow(DownloadList downloadList){
        this.list = downloadList;
        this.list.addEventListener(new DownloadListEventListener(){
            @Override
            public void downloadListUpdated() {
                updateList();
            }

            @Override
            public void downloadProgressReported() {
                frame.repaint();
            }
        });

        this.frame = new JDialog();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400,200));
        this.frame.setLayout(new BorderLayout());

        this.fileListView = new JList<DownloadItem>();
        this.fileListView.setCellRenderer(new DownloadItemRenderer());
        this.fileListView.setFixedCellWidth(this.fileListView.getWidth());
        this.fileListView.setOpaque(true);
        this.fileListView.setBackground(Color.decode(FILELIST_BACKGROUND));

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);

        this.clearButton = this.createButton("", "Clear List");

        /*
            TODO: action button actions shall be...

            1. "open file" for completed files
            2. "retry" for failed files
            3. "abort" for active files
        */
        this.actionButton = this.createButton("", "Abort");

        this.actionBar.setLayout(new BorderLayout());
        this.actionBar.setBorder(new EmptyBorder(8,16,10,16));
        this.actionBar.setBackground(Color.decode(ACTIONBAR_BACKGROUND));
        this.actionBar.add(this.clearButton,BorderLayout.EAST);
        this.actionBar.add(this.actionButton,BorderLayout.WEST);
        this.frame.add(this.actionBar, BorderLayout.SOUTH);

        /* hide the action button for empty lists */
        this.actionButton.setVisible(false);

        /* update the list for the first time */
        this.updateList();
    }

    public JPanel createButton(String iconText, String text){
        JPanel result = new JPanel();

        result.setLayout(new BorderLayout());
        result.setCursor(new Cursor(Cursor.HAND_CURSOR));
        result.setOpaque(false);

        /* icon for the button using the icon font */
        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setBorder(new EmptyBorder(0,0,0,6));
        iconLabel.setOpaque(false);
        iconLabel.setFont(ConfigurationManager.getIconFont(14f));
        iconLabel.setForeground(Color.decode(ACTIONBAR_INACTIVE_TEXTCOLOR));
        result.add(iconLabel,BorderLayout.WEST);

        /* text for the button using the default text font */
        JLabel textLabel = new JLabel(text);
        textLabel.setOpaque(false);
        textLabel.setFont(ConfigurationManager.getDefaultFont(12f));
        textLabel.setForeground(Color.decode(ACTIONBAR_INACTIVE_TEXTCOLOR));
        result.add(textLabel,BorderLayout.EAST);
        result.addMouseListener(new MouseAdapter() {
            /* notify the listeners of the forward move request */
            public void mouseReleased(MouseEvent evt){
                /* will be handled by another handler */
            }

            /* set the color to the hover color and use the hand cursor */
            public void mouseEntered(MouseEvent evt) {
                iconLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                iconLabel.setForeground(Color.decode(ACTIONBAR_TEXTCOLOR));
                textLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                textLabel.setForeground(Color.decode(ACTIONBAR_TEXTCOLOR));
            }
        
            /* revert back to the default cursor and default color */
            public void mouseExited(MouseEvent evt) {
                iconLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                iconLabel.setForeground(Color.decode(ACTIONBAR_INACTIVE_TEXTCOLOR));
                textLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                textLabel.setForeground(Color.decode(ACTIONBAR_INACTIVE_TEXTCOLOR));
            }
        });

        return result;
    }

    public void updateList(){
        this.data = this.list.getDownloadItemArray();

        int selectedIndex = this.fileListView.getSelectedIndex();
        this.fileListView.setListData(this.data);

        if(selectedIndex < this.data.length){
            this.fileListView.setSelectedIndex(selectedIndex);
        }else{
            if(this.data.length > 0){
                this.fileListView.setSelectedIndex(this.data.length-1);
            }
        }
    }

    public boolean isVisible(){
        return this.frame.isVisible();
    }

    public void hide(){
        this.frame.setVisible(false);
    }

    public void show(JFrame parent){
        this.updateList();
        this.frame.setLocationRelativeTo(parent);
        this.frame.setVisible(true);
    }
}