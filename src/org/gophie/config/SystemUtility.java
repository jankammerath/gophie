package org.gophie.config;

public class SystemUtility {
    public static String getFileSizeString(float byteSize){
        String result = byteSize + " Byte";

        if(byteSize > 1000){
            result = String.format("%.2f",(float)(byteSize / 1000)) + " KB";
        }if(byteSize > 1000000){
            result = String.format("%.2f",(float)(byteSize / 1000000)) + " MB";
        }if(byteSize > 1000000000){
            result = String.format("%.2f",(float)(byteSize / 1000000000)) + " GB";
        }

        return result;
    }
}