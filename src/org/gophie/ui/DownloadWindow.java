package org.gophie.ui;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.gophie.net.*;

public class DownloadWindow{
    /* local objects */
    private DownloadList list;
    private DownloadItem[] data;
    private ScheduledExecutorService schedule;

    /* local components */
    private JDialog frame;
    private JList<DownloadItem> fileListView;

    public DownloadWindow(DownloadList downloadList){
        this.list = downloadList;

        this.frame = new JDialog();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400,200));
        this.frame.setLayout(new BorderLayout());

        this.fileListView = new JList<DownloadItem>();
        this.fileListView.setCellRenderer(new DownloadItemRenderer());
        this.fileListView.setFixedCellWidth(this.fileListView.getWidth());
        this.fileListView.setOpaque(true);
        this.fileListView.setBackground(Color.decode("#1b1b1b"));

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);

        this.updateList();

        /* update the download list every second */
        this.schedule = Executors.newSingleThreadScheduledExecutor();
        this.schedule.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateList();
            }
            
        },0,1,TimeUnit.SECONDS);
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