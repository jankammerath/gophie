package org.gophie.ui;

import java.awt.*;
import javax.swing.*;

import org.gophie.net.DownloadItem;
import org.gophie.net.DownloadList;

public class DownloadWindow{
    /* local objects */
    private DownloadList list;

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
        this.fileListView.setOpaque(false);

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);

        this.updateList();
    }

    public void updateList(){
        this.fileListView.setListData(this.list.getDownloadItemArray());
    }

    public void show(JFrame parent){
        this.updateList();
        this.frame.setLocationRelativeTo(parent);
        this.frame.setVisible(true);
    }
}