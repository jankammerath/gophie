package org.gophie.ui;

import java.awt.*;
import javax.swing.*;

import org.gophie.net.*;

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
        this.fileListView.setFixedCellWidth(this.fileListView.getWidth());
        this.fileListView.setOpaque(true);
        this.fileListView.setBackground(Color.decode("#1b1b1b"));

        JScrollPane listScrollPane = new JScrollPane(this.fileListView);
        listScrollPane.setOpaque(false);
        listScrollPane.getViewport().setOpaque(false);
        this.frame.add(listScrollPane, BorderLayout.CENTER);

        this.updateList();
    }

    public void updateList(){
        // this.fileListView.setListData(this.list.getDownloadItemArray());
        /*

        TODO: this is test data !!!!
        
        */


        String[] textLines = {
            "9MahJongg_8932_EasyLongFileName_324_ItJustNeverEndsHere.prc	/users/tfurrows/files/PalmOS/MahJongg_8932_EasyLongFileName_324_ItJustNeverEndsHere.prc	sdf.org	70",
            "9McChords.prc	/users/tfurrows/files/PalmOS/McChords.prc	sdf.org	70",
            "9Poly.PRC	/users/tfurrows/files/PalmOS/Poly.PRC	gopher.firebladeservices.org	70",
            "9SFCave_0.03.prc	/users/tfurrows/files/PalmOS/SFCave_0.03.prc	sdf.org	70",
            "9Bunny2.PRC	/users/tfurrows/files/PalmOS/Bunny2.PRC	sdf.org	70",
            "9Afterburner3.1.zip	/users/tfurrows/files/PalmOS/Afterburner3.1.zip	sdf.org	70"
        };

        DownloadItem[] itemList = new DownloadItem[textLines.length];
        for(int i=0; i<textLines.length; i++){
            DownloadItem sample = new DownloadItem();
            sample.setGopherItem(new GopherItem(textLines[i]));
            itemList[i] = sample;
        }

        this.fileListView.setListData(itemList);

        /* 
        
        TODO: end of test data !!!

        */
    }

    public void show(JFrame parent){
        this.updateList();
        this.frame.setLocationRelativeTo(parent);
        this.frame.setVisible(true);
    }
}