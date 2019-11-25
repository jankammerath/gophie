package org.gophie.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import org.gophie.net.*;
import org.gophie.config.*;
import org.gophie.net.DownloadItem.DownloadStatus;

public class DownloadItemRenderer extends JPanel implements ListCellRenderer<DownloadItem> {
    private static final long serialVersionUID = 1L;

    private JLabel titleLabel = new JLabel();
    private JLabel textLabel = new JLabel();

    @Override
    public Component getListCellRendererComponent(JList<? extends DownloadItem> list, 
                                                DownloadItem value, int index,
                                                boolean isSelected, boolean cellHasFocus) {
        /* get the config file for the color schemes */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        /* render the cell for this download item */
        this.setOpaque(false);
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(new EmptyBorder(5,10,5,10));

        /* highlight if this element is selected */
        if(isSelected == true){
            this.setOpaque(true);
            this.setBackground(Color.decode(configFile.getSetting
                ("DOWNLOAD_SELECTED_COLOR", "Appearance", "#cf9a0c")));
        }

        /* get the gopher item of this download */
        GopherItem item = value.getGopherItem();

        /* show the file name in the title */
        this.titleLabel.setText(item.getFileName());
        Font titleFont = ConfigurationManager.getConsoleFont(15f);
        this.titleLabel.setFont(titleFont.deriveFont(titleFont.getStyle() | Font.BOLD));
        this.titleLabel.setForeground(Color.decode(configFile.getSetting
                    ("DOWNLOAD_TITLE_COLOR", "Appearance", "#ffffff")));

        /* create the information text based on the status */
        String statusText = "Download not started";
        String byteLoadedText = SystemUtility.getFileSizeString(value.getByteCountLoaded());

        /* show message for completed downloads */
        if(value.getStatus() == DownloadStatus.COMPLETED){
            statusText = "Completed (" + byteLoadedText + ")";
        }
        
        /* show message for active downloads */
        if(value.getStatus() == DownloadStatus.ACTIVE){
            String transferRate = SystemUtility.getFileSizeString(value.getBytePerSecond());
            statusText = byteLoadedText + " (" + transferRate + "/sec)";
        }
        
        /* show message for failed downloads */
        if(value.getStatus() == DownloadStatus.FAILED){
            statusText = "Failed (" + byteLoadedText + ")";
        }

        /* append the host name to the info text */
        statusText += " — " + item.getHostName();

        /* set the text to the status text label */
        this.textLabel.setText(statusText);
        this.textLabel.setBorder(new EmptyBorder(4,0,0,0));
        this.textLabel.setForeground(Color.decode(configFile.getSetting
                    ("DOWNLOAD_TEXT_COLOR", "Appearance", "#e0e0e0")));
        Font textFont = ConfigurationManager.getConsoleFont(13f);
        this.textLabel.setFont(textFont);

        this.add(this.titleLabel);
        this.add(this.textLabel);

        return this;
    }
}