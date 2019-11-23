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
    private JList<String> fileListView;

    public DownloadWindow(DownloadList downloadList){
        this.list = downloadList;

        this.frame = new JDialog();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400,200));
        this.frame.setLayout(new BorderLayout());

        this.fileListView = new JList<String>();
        this.fileListView.setOpaque(false);

        this.fileListView.setListData(new String[]{
            "What", "The", "Fuck", "Is", "This",
            "What", "The", "Fuck", "Is", "This",
            "What", "The", "Fuck", "Is", "This",
            "What", "The", "Fuck", "Is", "This",
            "What", "The", "Fuck", "Is", "This",
            "What", "The", "Fuck", "Is", "This"
        });

        // this.fileListView.add(new DownloadItemView(null));
        // this.fileListView.add(new DownloadItemView(null));
        // this.fileListView.add(new DownloadItemView(null));
        // itemContainer.add(new DownloadItemView(null));

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);
    }

    private void updateList(){
        for(DownloadItem fileItem: this.list){
            // process the list here
        }
    }

    public void show(JFrame parent){
        this.updateList();
        this.frame.setLocationRelativeTo(parent);
        this.frame.setVisible(true);
    }
}