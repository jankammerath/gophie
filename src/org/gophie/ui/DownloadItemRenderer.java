package org.gophie.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import org.gophie.net.*;
import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.config.ConfigurationManager;
import org.gophie.config.SystemUtility;

public class DownloadItemRenderer extends JPanel implements ListCellRenderer<DownloadItem> {
    private static final long serialVersionUID = 1L;

    private JLabel titleLabel = new JLabel();
    private JLabel textLabel = new JLabel();

    @Override
    public Component getListCellRendererComponent(JList<? extends DownloadItem> list, 
                                                DownloadItem value, int index,
                                                boolean isSelected, boolean cellHasFocus) {
        /* render the cell for this download item */
        this.setOpaque(false);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(5,10,5,10));

        /* highlight if this eleemtn is selected */
        if(isSelected == true){
            this.setOpaque(true);
            this.setBackground(Color.decode("#248AC2"));
        }

        /* get the gopher item of this download */
        GopherItem item = value.getGopherItem();

        /* show the file name in the title */
        this.titleLabel.setText(item.getFileName());
        Font titleFont = ConfigurationManager.getConsoleFont(15f);
        this.titleLabel.setFont(titleFont.deriveFont(titleFont.getStyle() | Font.BOLD));
        this.titleLabel.setForeground(Color.decode("#ffffff"));

        /* create the information text based on the status */
        String statusText = "Download not started";
        String byteLoadedText = SystemUtility.getFileSizeString(value.getByteCountLoaded());

        if(value.getStatus() == DownloadStatus.COMPLETED){
            statusText = "Completed (" + byteLoadedText + ")";
        }

        /* append the host name to the info text */
        statusText += " — " + item.getHostName();

        /* set the text to the status text label */
        this.textLabel.setText(statusText);
        this.textLabel.setBorder(new EmptyBorder(4,0,0,0));
        this.textLabel.setForeground(Color.decode("#e0e0e0"));
        Font textFont = ConfigurationManager.getConsoleFont(13f);
        this.textLabel.setFont(textFont);

        this.add(this.titleLabel);
        this.add(this.textLabel);

        return this;
    }
}