package org.gophie.ui;

import java.awt.*;
import javax.swing.*;

import org.gophie.net.DownloadItem;

public class DownloadItemView extends JLabel{
    private static final long serialVersionUID = 1L;

    /* private components */
    private JLabel fileName = new JLabel("unknown.dat");
    private JLabel sourceHost = new JLabel("gopher.domain.tld");
    private JLabel fileSize = new JLabel("3.45 MB");
    private JLabel bitRate = new JLabel("978 Kbps");

    /* private variables */
    private DownloadItem item;
    
    public DownloadItemView(DownloadItem sourceItem){
        this.item = sourceItem;

        this.setBackground(Color.decode("#000000"));
        this.setLayout(new FlowLayout());
        this.add(this.fileName);
        this.add(this.sourceHost);
        this.add(this.fileSize);
        this.add(this.bitRate);

        this.setVisible(true);
    }
}