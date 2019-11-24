package org.gophie.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import org.gophie.net.*;
import org.gophie.net.DownloadItem.DownloadStatus;
import org.gophie.config.SystemUtility;

public class DownloadItemRenderer extends JPanel implements ListCellRenderer<DownloadItem> {
    private static final long serialVersionUID = 1L;

    private JLabel fileName = new JLabel();
    private JLabel fileSource = new JLabel();
    private JLabel fileSize = new JLabel();
    private JLabel fileStatus = new JLabel();

    @Override
    public Component getListCellRendererComponent(JList<? extends DownloadItem> list, 
                                                DownloadItem value, int index,
                                                boolean isSelected, boolean cellHasFocus) {
        /* render the cell for this download item */
        this.setOpaque(false);
        this.setLayout(new GridLayout(2,2));
        this.setBorder(new EmptyBorder(5,5,5,5));

        /* get the gopher item of this download */
        GopherItem item = value.getGopherItem();
        this.fileName.setText(item.getFileName());
        this.fileSource.setText(item.getHostName());
        this.fileSize.setText(SystemUtility.getFileSizeString(value.getByteCountLoaded()));

        if(value.getStatus() == DownloadStatus.ACTIVE){
            this.fileStatus.setText("Downloading...");
        }if(value.getStatus() == DownloadStatus.COMPLETED){
            this.fileStatus.setText("Completed");
        }if(value.getStatus() == DownloadStatus.FAILED){
            this.fileStatus.setText("Failed");
        }

        this.add(this.fileName,0);
        this.add(this.fileSize,1);
        this.add(this.fileSource,2);
        this.add(this.fileStatus,3);

        return this;
    }
}