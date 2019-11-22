package org.gophie.config;

import java.awt.*;
import java.io.File;

public class ConfigurationManager{
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
            result = Font.createFont(Font.TRUETYPE_FONT, new File("../resources/Fonts/Feather.ttf")).deriveFont(size);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(result);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the fonts!");
        }

        return result;
    }

    public static Font getConsoleFont(float size){
        Font result = null;
        
        try{
            result = Font.createFont(Font.TRUETYPE_FONT, new File("../resources/Fonts/Inconsolata-Regular.ttf")).deriveFont(17f);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(result);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the fonts!");
        }

        return result;
    }
}