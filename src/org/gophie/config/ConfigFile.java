package org.gophie.config;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;

/**
 * This class parses *.INI configuration files
 * as defined in: https://en.wikipedia.org/wiki/INI_file
 */
public class ConfigFile {
    /* full file name of this config file */
    private String fileName = "";

    /* hashmap with sections and settings */
    HashMap<String, HashMap<String, String>> config;

    /**
     * Creates a new instance of an *.INI config file
     * 
     * @param configFileName
     * the full file name of the config file
     */
    public ConfigFile(String configFileName){
        this.config = new HashMap<String, HashMap<String, String>>();
        this.fileName = configFileName;
        this.parse();
    }

    /**
     * Gets a setting from the config map
     * 
     * @param name
     * Name of the setting
     * 
     * @param section
     * Section the setting is in
     * 
     * @param defaultValue
     * Default value to return
     * 
     * @return
     * Returns the setting value or the default 
     * value if the setting or its section is not present
     */
    public String getSetting(String name, String section, String defaultValue){
        String result = defaultValue;

        if(this.config.containsKey(section)){
            if(this.config.get(section).containsKey(name)){
                result = this.config.get(section).get(name);
            }
        }

        return result;
    }

    /**
     * Adds a setting to the current config map
     * 
     * @param name
     * Name of the setting
     * 
     * @param value
     * Value of the setting
     * 
     * @param section
     * Name of the section
     */
    public void setSetting(String name, String value, String section){
        /* avoid adding empty values to the config map */
        if(section.length() > 0 && name.length() > 0 && value.length() > 0){
            /* create a new hashmap with settings for this section */
            HashMap<String, String> settingMap = new HashMap<String, String>();

            if(this.config.containsKey(section)){
                /* get the current setting map for this section */
                settingMap = this.config.get(section);
            }

            /* put name and value to the setting map */
            settingMap.put(name, value);
            this.config.put(section, settingMap);
        }
    }

    /**
     * Parse the configuration file
     */
    private void parse(){
        try{
            /* make sure that config file exists */
            if(Files.exists(Paths.get(this.fileName))){
                /* read all lines from the defined file */
                List<String> lineList = Files.readAllLines(Paths.get(this.fileName),Charset.defaultCharset());

                /* go through each line */
                String configSection = "NONE";
                for (String line : lineList) {
                    /* check the type of line */
                    String value = line.trim();
                    if(value.length() > 0){
                        /* ignore comments */
                        if(!value.startsWith(";")){
                            /* check if this is a section */
                            if(value.startsWith("[") == true && value.endsWith("]")){
                                /* this is a section, track it to assign values */
                                configSection = value.substring(1, value.length()-1);
                            }

                            /* check if this is a value assignment */
                            if(value.indexOf("=") > 0){
                                String[] setting = value.split("=");
                                if(setting.length == 2){
                                    /* apply the setting to the config */
                                    this.setSetting(setting[0].trim(), setting[1].trim(), configSection);
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception ex){
            /* failed to parse config file */
            System.out.println("Failed to open and parse file (" 
                        + this.fileName + "): " + ex.getMessage());
        }
    }
}