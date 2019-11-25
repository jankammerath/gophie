package org.gophie.config;

import java.awt.*;
import java.io.*;
import javax.swing.ImageIcon;

public class ConfigurationManager{
    private static ConfigFile configFile;
    private static final String MAIN_CONFIG_FILENAME = "config.ini";
    private static final String CONFIG_FOLDERNAME = "Gophie";

    /**
     * Returns the main configuration file
     * 
     * @return
     * The main configuration file as ConfigFile
     */
    public static ConfigFile getConfigFile(){
        if(ConfigurationManager.configFile == null){
            String configFileName = ConfigurationManager.getConfigPath() + MAIN_CONFIG_FILENAME;
            ConfigurationManager.configFile = new ConfigFile(configFileName);
        }

        return ConfigurationManager.configFile;
    }

    /**
     * Returns the configuration directory's path
     * 
     * @return
     * The full path to the configuration directory
     */
    public static String getConfigPath(){
        /* define the full path of the configuration directory */
        String result = System.getProperty("user.home") + "/" + CONFIG_FOLDERNAME + "/";

        /* make sure all the directories exist */
        File mainConfigFile = new File(result + MAIN_CONFIG_FILENAME);
        mainConfigFile.getParentFile().mkdirs();

        return result;
    }

    /**
     * Returns a Font from the resources
     * 
     * @param fileName
     * Filename of the font in the resources path
     * 
     * @param size
     * Size of the Font
     * 
     * @return
     * The Font object with the font
     */
    public static Font getFont(String fileName, float size){
        Font result = null;

        try{
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            result = Font.createFont(Font.TRUETYPE_FONT, classLoader.getResourceAsStream("res/" + fileName)).deriveFont(size);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(result);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load font: " + ex.getMessage());
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
        return ConfigurationManager.getFont("Feather.ttf", size);
    }

    /**
     * Returns an image icon from the resources
     * 
     * @param name
     * Name of the image icon file from the resources
     * 
     * @return
     * The ImageIcon object from the resources
     */
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

    /**
     * Returns the default console font
     * 
     * @param size
     * Size of the requested font
     * 
     * @return
     * The Font object
     */
    public static Font getConsoleFont(float size){
        return ConfigurationManager.getFont("Inconsolata-Regular.ttf", size);
    }

    /**
     * Returns the default text font
     * 
     * @param size
     * Size of the requested font
     * 
     * @return
     * The Font object
     */
    public static Font getDefaultFont(float size){
        return ConfigurationManager.getFont("OpenSans-Regular.ttf", size);
    }

    /**
     * Returns the directory where all
     * the downloads should be stored in
     * which usually resides in the user 
     * home
     * 
     * @return
     * Path to the download directory
     */
    public static String getDownloadPath(){
        return System.getProperty("user.home") + "/Downloads/";
    }
}