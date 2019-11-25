package org.gophie.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gophie.config.*;
import org.gophie.net.*;
import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.net.event.*;
import org.gophie.ui.event.ActionButtonEventListener;

public class DownloadWindow implements ActionButtonEventListener {
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
    private ActionButton clearButton;
    private ActionButton actionButton;

    public DownloadWindow(DownloadList downloadList) {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        this.list = downloadList;
        this.list.addEventListener(new DownloadListEventListener() {
            @Override
            public void downloadListUpdated() {
                updateList();
            }

            @Override
            public void downloadProgressReported() {
                handleSelectionChange();
                frame.repaint();
            }
        });

        this.frame = new JDialog();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400, 200));
        this.frame.setLayout(new BorderLayout());

        this.fileListView = new JList<DownloadItem>();
        this.fileListView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.fileListView.setCellRenderer(new DownloadItemRenderer());
        this.fileListView.setFixedCellWidth(this.fileListView.getWidth());
        this.fileListView.setOpaque(true);
        this.fileListView.setBackground(Color.decode(configFile.getSetting
                ("FILELIST_BACKGROUND", "Appearance", FILELIST_BACKGROUND)));

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);

        this.clearButton = new ActionButton("", "Clear List",
            configFile.getSetting("ACTIONBAR_TEXTCOLOR", "Appearance", ACTIONBAR_TEXTCOLOR),
            configFile.getSetting("ACTIONBAR_INACTIVE_TEXTCOLOR", "Appearance", ACTIONBAR_INACTIVE_TEXTCOLOR)
        );
        this.clearButton.setButtonEnabled(false);
        this.clearButton.setButtonId(1);
        this.clearButton.addEventListener(this);

        this.actionButton = new ActionButton("", "Abort",
            configFile.getSetting("ACTIONBAR_TEXTCOLOR", "Appearance", ACTIONBAR_TEXTCOLOR),
            configFile.getSetting("ACTIONBAR_INACTIVE_TEXTCOLOR", "Appearance", ACTIONBAR_INACTIVE_TEXTCOLOR)
        );
        this.actionButton.setButtonId(0);
        this.actionButton.addEventListener(this);

        this.actionBar.setLayout(new BorderLayout());
        this.actionBar.setBorder(new EmptyBorder(8, 16, 10, 16));
        this.actionBar.setBackground(Color.decode(configFile.getSetting
            ("ACTIONBAR_BACKGROUND", "Appearance", ACTIONBAR_BACKGROUND)));
        this.actionBar.add(this.clearButton, BorderLayout.EAST);
        this.actionBar.add(this.actionButton, BorderLayout.WEST);
        this.frame.add(this.actionBar, BorderLayout.SOUTH);

        /* hide the action button for empty lists */
        this.actionButton.setVisible(false);

        this.fileListView.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleSelectionChange();
            }        
        });

        /* update the list for the first time */
        this.updateList();
    }

    private void handleSelectionChange(){
        DownloadItem selected = this.fileListView.getSelectedValue();
        if(selected == null){
            this.actionButton.setVisible(false);
        }else{
            if(selected.getStatus() == DownloadStatus.ACTIVE){
                this.actionButton.setContent("","Abort");
            }if(selected.getStatus() == DownloadStatus.FAILED){
                this.actionButton.setContent("","Retry");
            }if(selected.getStatus() == DownloadStatus.COMPLETED){
                this.actionButton.setContent("","Open");
            }if(selected.getStatus() == DownloadStatus.IDLE){
                this.actionButton.setContent("","Start");
            }

            this.actionButton.setVisible(true);
            this.actionButton.setButtonEnabled(true);
        }
    
        /* disable the clear list button for empty lists */
        if(this.list.hasNonActiveItems()){
            this.clearButton.setButtonEnabled(true);
        }else{
            this.clearButton.setButtonEnabled(false);
        }
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

        this.handleSelectionChange();
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

    @Override
    public void buttonPressed(int buttonId) {
        if(buttonId == 0){
            /* the action button */
            DownloadItem item = this.fileListView.getSelectedValue();
            if(item.getStatus() == DownloadStatus.ACTIVE){
                /* cancel the currently active item */
                item.cancel();

                /* remove the item from the list */
                this.list.remove(item);

                /* delete the file form disk */
                item.deleteFile();
            }if(item.getStatus() == DownloadStatus.FAILED){
                /* retry failed item */
                item.start();
            }if(item.getStatus() == DownloadStatus.COMPLETED){
                /* open completed item file */
                item.openFileOnDesktop();
            }if(item.getStatus() == DownloadStatus.IDLE){
                /* start item in idle item */
                item.start();
            }
        }if(buttonId == 1){
            /* the clear list button */
            this.list.clearNonActiveItems();
        }

        /* update our local list */
        this.updateList();
    }
}