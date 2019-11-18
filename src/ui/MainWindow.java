package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MainWindow{
    /* define the constants for the UI */
    public static final String NAVIGATIONBAR_BACKGROUND = "#248AC2";
    public static final String NAVIGATIONBAR_TEXTCOLOR = "#76bce3";
    public static final String NAVIGATIONBAR_TEXTHOVERCOLOR = "#ffffff";
    public static final String VIEW_BACKGROUND = "#1b1b1b";

    /* define the local objects */
    JFrame frame;
    JEditorPane viewPane;
    JScrollPane viewScrollPane;
    JMenuBar menuBar;
    NavigationBar navigationBar;

    public MainWindow(){
        try{
            /* try setting to system look and feel */
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception e){
            /* exception when trying to set, but ignore it */
            System.out.println("Error setting system look and feel");
        }

        /* remove the borders for the pane */
        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.getDefaults().put("ScrollPane.border", BorderFactory.createEmptyBorder());

        /* create the main window */
        this.frame = new JFrame("Gophie");
        this.frame.setMinimumSize(new Dimension(800, 600));
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* create the editor pane */
        this.viewPane = new JEditorPane();
        this.viewPane.setEditable(false);
        this.viewPane.setBackground(Color.decode(VIEW_BACKGROUND));
        this.viewScrollPane = new JScrollPane(this.viewPane);
        this.viewScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.viewScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        /* create the menu bar */
        this.createMenuBar();

        /* create the navigation bar */
        this.navigationBar = new NavigationBar(NAVIGATIONBAR_BACKGROUND, 
                NAVIGATIONBAR_TEXTCOLOR, NAVIGATIONBAR_TEXTHOVERCOLOR);

        /* set the content pane */
        Container contentPane = frame.getContentPane();
        contentPane.add(this.viewScrollPane, BorderLayout.CENTER);
        contentPane.add(this.navigationBar, BorderLayout.SOUTH);
        this.frame.setVisible(true);
    }

    private void createMenuBar(){
        /* set to use the mac menu bar instead */
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        /* set the proper application title on mac */
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Gophie");

        /* create the JMenuBar */
        this.menuBar = new JMenuBar();
        JMenu workspaceMenu = new JMenu("Workspace");

        /* create the open menu item */
        JMenuItem openMenuItem = new JMenuItem(new AbstractAction("Open Workspace") {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null,"Ok");
            }
        });
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        workspaceMenu.add(openMenuItem);

        /* create the save menu item */
        JMenuItem saveMenuItem = new JMenuItem("Save Workspace");
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        workspaceMenu.add(saveMenuItem);

        /* show the close menu item at the bottom */
        workspaceMenu.add(new JSeparator());
        JMenuItem closeMenuItem = new JMenuItem("Close Workspace");
        workspaceMenu.add(closeMenuItem);
        this.menuBar.add(workspaceMenu);

        this.frame.setJMenuBar(this.menuBar);
    }

    public void show(){
        /* display the window */
        this.frame.pack();
        this.frame.setVisible(true);
    }
}