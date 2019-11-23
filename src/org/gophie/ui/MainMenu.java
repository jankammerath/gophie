package org.gophie.ui;

import javax.swing.*;

public class MainMenu extends JMenuBar {
    /* constants */
    private static final long serialVersionUID = 1L;

    /*
        Constructors builds up the menu with all
        menu items required for the main menu
    */
    public MainMenu(){
       /* set to use the mac menu bar instead */
       System.setProperty("apple.laf.useScreenMenuBar", "true");

       /* set the proper application title on mac */
       System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Gophie");

       JMenu viewMenu = new JMenu("View");
       JMenuItem viewDownloadItem = new JMenuItem("Downloads");
       viewMenu.add(viewDownloadItem);
       this.add(viewMenu);
    }
}