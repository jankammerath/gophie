package org.gophie.ui.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import org.gophie.net.GopherItem;
import org.gophie.config.*;

public class DownloadPromptDialog {
    /* local objects */
    GopherItem fileItem;

    /* local components */
    private JDialog dialog;
    private JFrame parent;
    private JLabel titleLabel = new JLabel("Do you want to open or save the item?");
    private JLabel fileNameLabel = new JLabel("unknown.dat");
    private JLabel fileTypeLabel = new JLabel("Unknown type");
    private JLabel fileUrlLabel = new JLabel("gopher.domain.tld");
    private JButton openButton = new JButton("Open");
    private JButton saveButton = new JButton("Save");
    private JButton cancelButton = new JButton("Cancel");

    public DownloadPromptDialog(JFrame parentFrame) {
        this.parent = parentFrame;

        /* build up the dialog itself */
        this.dialog = new JDialog(parentFrame);
        this.dialog.setResizable(false);
        this.dialog.setSize(new Dimension(400, 200));
        this.dialog.setLayout(null);

        this.titleLabel.setBounds(15, 10, 380, 20);
        this.dialog.getContentPane().add(this.titleLabel);

        Font fileNameFont = this.fileNameLabel.getFont();
        this.fileNameLabel.setFont(fileNameFont.deriveFont(fileNameFont.getStyle() | Font.BOLD));
        this.fileNameLabel.setBounds(15, 40, 360, 20);
        this.dialog.getContentPane().add(this.fileNameLabel);

        JLabel fileTypeTitleLabel = new JLabel("Type:");
        fileTypeTitleLabel.setBounds(15, 75, 50, 20);
        this.dialog.getContentPane().add(fileTypeTitleLabel);

        this.fileTypeLabel.setBounds(70, 75, 290, 20);
        this.dialog.getContentPane().add(this.fileTypeLabel);

        JLabel fileSourceTitleLabel = new JLabel("From:");
        fileSourceTitleLabel.setBounds(15, 95, 50, 20);
        this.dialog.getContentPane().add(fileSourceTitleLabel);

        Font fileUrlFont = this.fileUrlLabel.getFont();
        this.fileUrlLabel.setFont(fileUrlFont.deriveFont(fileUrlFont.getStyle() | Font.BOLD));
        this.fileUrlLabel.setBounds(70, 95, 290, 20);
        this.dialog.getContentPane().add(this.fileUrlLabel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.setBounds(15, 130, 375, 40);
        buttonPanel.add(this.openButton);
        buttonPanel.add(this.saveButton);
        buttonPanel.add(this.cancelButton);
        this.dialog.getContentPane().add(buttonPanel);

        /* just throw the file into the temp 
            folder when user wants to open it */
        this.openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println(System.getProperty("user.home") + "/Downloads/");
                dialog.setVisible(false);
            }
        });

        /* prompt for storage location when asking to save  */
        this.saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /* close first to prevent overlap */
                dialog.setVisible(false);
                
                /* present file chooser and let user pick the target */
                FileDialog fileDialog = new FileDialog(new Frame(), "Save file", FileDialog.SAVE);
                fileDialog.setFile("Untitled.txt");
                fileDialog.setVisible(true);
            }
        });

        /* simply close the window when cancel was hit */
        this.cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
    }

    public void display(GopherItem item){
        /* set the item locally */
        this.fileItem = item;

        /* update text and title with file name */
        String itemFileName = item.getFileName();
        this.fileNameLabel.setText(itemFileName);

        String fileExt = item.getFileExt();
        Icon fileTypeIcon = SystemUtility.getFileExtIcon(fileExt);
        this.fileNameLabel.setIcon(fileTypeIcon);
        this.fileUrlLabel.setText(item.getHostName());
        this.fileTypeLabel.setText(item.getTypeName() 
                + " (" + fileExt.toUpperCase() + ")");
        this.dialog.setTitle(itemFileName);

        /* show the dialog */
        this.dialog.setLocationRelativeTo(this.parent);
        this.dialog.setModal(true);
        this.dialog.setVisible(true);
    }
}