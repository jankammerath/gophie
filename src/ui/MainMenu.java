package ui;

import java.awt.*;
import java.awt.event.*;
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

       /* create the JMenuBar */
       JMenu pageMenu = new JMenu("Page");

       /* create the open menu item */
       JMenuItem openMenuItem = new JMenuItem(new AbstractAction("Open Page...") {
           private static final long serialVersionUID = 1L;
           public void actionPerformed(ActionEvent e) {
               JOptionPane.showMessageDialog(null,"Ok");
           }
       });
       openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O',
           Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
           pageMenu.add(openMenuItem);

       /* create the save menu item */
       JMenuItem saveMenuItem = new JMenuItem("Save Page");
       saveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S',
           Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
       
       pageMenu.add(saveMenuItem);
       pageMenu.add(new JMenuItem("Save Page As..."));

       /* show the close menu item at the bottom */
       pageMenu.add(new JSeparator());
       JMenuItem closeMenuItem = new JMenuItem("Close Page");
       pageMenu.add(closeMenuItem);
       this.add(pageMenu);

       JMenu navigationMenu = new JMenu("Navigation");
       navigationMenu.add(new JMenuItem("Home Gopher"));
       navigationMenu.add(new JSeparator());
       navigationMenu.add(new JMenuItem("Backward"));
       navigationMenu.add(new JMenuItem("Forward"));
       navigationMenu.add(new JSeparator());
       navigationMenu.add(new JMenuItem("Refresh Page"));
       navigationMenu.add(new JSeparator());
       navigationMenu.add(new JMenuItem("Set As Home"));
       this.add(navigationMenu);

       JMenu historyMenu = new JMenu("History");
       historyMenu.add("Clear History");
       historyMenu.add(new JSeparator());
       historyMenu.add("gopher.floodgap.com/WHY_THIS.txt");
       historyMenu.add("gopher.floodgap.com/");
       historyMenu.add("gopher.kammerath.net/news");
       historyMenu.add("gopher.kammerath.net/");
       this.add(historyMenu);

       JMenu configMenu = new JMenu("Settings");
       configMenu.add(new JMenuItem("Appearance"));
       configMenu.add(new JMenuItem("Cache"));
       this.add(configMenu);
    }
}