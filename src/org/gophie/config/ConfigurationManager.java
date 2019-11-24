package org.gophie.config;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.ImageIcon;

public class ConfigurationManager{
    public static String getConfigPath(){
        String result = System.getProperty("user.home") + "/.Gophie/";

        /* make sure all the directories exist */
        File mainConfigFile = new File(result + "config.ini");
        mainConfigFile.getParentFile().mkdirs();

        return result;
    }

    public static String getConfigFile(String fileName){
        String result = "";

        try{
            String configFileName = ConfigurationManager.getConfigPath() + fileName;
            result = new String(Files.readAllBytes(Paths.get(configFileName)));    
        }catch(Exception ex){
            /* output the message to the console */
            System.out.println("Failed to read file (" 
                + fileName + "): " + ex.getMessage());
        }       

        return result;
    }

    public static Boolean saveConfigFile(String fileName, String content){
        Boolean result = false;

        try{
            File configFile = new File(ConfigurationManager.getConfigPath() + fileName);
            FileWriter writer = new FileWriter(configFile);
            writer.write(content);
            writer.close();
            result = true;
        }catch(Exception ex){
            /* output the message to the console */
            System.out.println("Failed to write file (" 
                + fileName + "): " + ex.getMessage());
        }
    
        return result;
    }

    /**
     * Returns the font for icon display
     * 
     * @param size
     * The size of the font as float
     * 
     * @return
     * A Font object containing the icon font
     */
    public static Font getIconFont(float size){
        Font result = null;

        /* get the icon font for this navigation bar */
        try{
            /* try to open the font for icon display */
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            result = Font.createFont(Font.TRUETYPE_FONT, classLoader.getResourceAsStream("res/Feather.ttf")).deriveFont(size);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(result);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the icon font: " + ex.getMessage());
        }

        return result;
    }

    public static ImageIcon getImageIcon(String name){
        ImageIcon result = null;

        try{
            /* try to open the font for icon display */
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            result = new ImageIcon(classLoader.getResource("res/"+name));            
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the image icon (" + name + "): " + ex.getMessage());
        }

        return result;
    }

    public static Font getConsoleFont(float size){
        Font result = null;
        
        try{
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            result = Font.createFont(Font.TRUETYPE_FONT, classLoader.getResourceAsStream("res/Inconsolata-Regular.ttf")).deriveFont(17f);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(result);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the text font:" + ex.getMessage());
        }

        return result;
    }

    public static String getDownloadPath(){
        return System.getProperty("user.home") + "/Downloads/";
    }
}