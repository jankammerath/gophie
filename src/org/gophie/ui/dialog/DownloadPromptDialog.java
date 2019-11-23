package org.gophie.ui.dialog;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import org.gophie.net.GopherItem;

public class DownloadPromptDialog {
    /* local variables */
    private GopherItem fileItem;

    /* local components */
    private JDialog dialog;
    private JPanel contentPanel;
    private JLabel titleLabel;

    public DownloadPromptDialog(){
        /* build up the dialog itself */
        this.dialog = new JDialog();
        this.dialog.setSize(new Dimension(320, 240));
        this.dialog.setResizable(false);

        /* define the content panel */
        this.contentPanel = new JPanel();
        this.contentPanel.setBorder(new EmptyBorder(10,10,10,10));

        /* create the title label */
        this.titleLabel = new JLabel("What to do with that item?");
        this.contentPanel.add(titleLabel);

        /* add the bunch to the dialog */
        this.dialog.add(this.contentPanel);
    }

    private void setItem(GopherItem item){
        this.fileItem = item;
    }

    public void display(GopherItem item){
        this.setItem(item);

        this.dialog.pack();
        this.dialog.setVisible(true);
    }

    public static void prompt(GopherItem item){
        DownloadPromptDialog promptDialog = new DownloadPromptDialog();
        promptDialog.display(item);
        
    }
}