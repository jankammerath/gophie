package org.gophie.ui;

import java.awt.*;
import javax.swing.*;

import org.gophie.net.DownloadList;

public class DownloadWindow{
    /* local objects */
    private DownloadList list;

    /* local components */
    private JFrame frame;
    private JScrollPane contentPane;

    public DownloadWindow(DownloadList downloadList){
        this.list = downloadList;

        this.frame = new JFrame();
        this.frame.setTitle("Downloads");
        this.frame.setMinimumSize(new Dimension(400,200));

        this.contentPane = new JScrollPane();
        this.frame.setContentPane(this.contentPane);
    }

    private void updateList(){

    }

    public void show(JFrame parent){
        this.frame.setLocationRelativeTo(parent);
        this.frame.setVisible(true);
    }
}