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
    private JScrollPane contentPane;

    public DownloadWindow(DownloadList downloadList){
        this.list = downloadList;

        this.frame = new JDialog();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400,200));
        this.frame.setLayout(new BorderLayout());

        this.contentPane = new JScrollPane();
        this.contentPane.setOpaque(false);
        this.contentPane.getViewport().setOpaque(false);

        this.contentPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.contentPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.frame.add(this.contentPane,BorderLayout.CENTER);
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