package org.gophie.config;

import java.io.File;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

public class SystemUtility {
    public static Icon getFileExtIcon(String fileExt){
        Icon result = null;

        try{
            File tempFile = File.createTempFile("gophericonextfile", "."+fileExt);
            result = FileSystemView.getFileSystemView().getSystemIcon(tempFile);
            tempFile.delete();
        }catch(Exception ex){
            /* failed to acquire icon */
            System.out.println("Could not get icon for ext: " + ex.getMessage());
        }

        return result;
    }
}