package ui;

import java.awt.*;
import javax.swing.*;

/* import gopher network client */
import net.GopherClient;
import net.GopherNetworkException;
import net.GopherPage;
import net.event.GopherClientEventListener;
import net.event.GopherError;

/* import ui event listeners */
import ui.event.NavigationInputListener;

public class MainWindow implements NavigationInputListener, GopherClientEventListener {
    /* define the constants for the UI */
    public static final String APPLICATION_TITLE = "Gophie";
    public static final String NAVIGATIONBAR_BACKGROUND = "#248AC2";
    public static final String NAVIGATIONBAR_TEXTCOLOR = "#76bce3";
    public static final String NAVIGATIONBAR_TEXTHOVERCOLOR = "#ffffff";
    public static final String VIEW_BACKGROUND = "#1b1b1b";
    public static final String DEFAULT_GOPHERHOME = "gopher.floodgap.com";

    /* local network objects */
    private GopherClient gopherClient;

    /* local ui elements */
    private JFrame frame;
    private PageView pageView;
    private NavigationBar navigationBar;

    public MainWindow() {
        /* create the instance of the client */
        this.gopherClient = new GopherClient();

        try {
            /* try setting to system look and feel */
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            /* exception when trying to set, but ignore it */
            System.out.println("Error setting system look and feel");
        }

        /* remove the borders for the pane */
        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.getDefaults().put("ScrollPane.border", BorderFactory.createEmptyBorder());

        /* create the main window */
        this.frame = new JFrame(APPLICATION_TITLE);
        this.frame.setMinimumSize(new Dimension(800, 600));
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /* create the page view component object */
        this.pageView = new PageView(VIEW_BACKGROUND);

        /* create the menu bar */
        this.frame.setJMenuBar(new MainMenu());

        /* create the navigation bar */
        this.navigationBar = new NavigationBar(NAVIGATIONBAR_BACKGROUND, NAVIGATIONBAR_TEXTCOLOR,
                NAVIGATIONBAR_TEXTHOVERCOLOR);
        this.navigationBar.setAddressText(DEFAULT_GOPHERHOME);

        /* attach listener to navigation bar */
        this.navigationBar.addListener(this);

        /* set the content pane */
        Container contentPane = frame.getContentPane();
        contentPane.add(this.pageView, BorderLayout.CENTER);
        contentPane.add(this.navigationBar, BorderLayout.SOUTH);
        this.frame.setVisible(true);
    }

    public void show() {
        /* display the window */
        this.frame.pack();
        this.frame.setVisible(true);
    }

    @Override
    public void addressRequested(String addressText) {
        this.gopherClient.fetchAsync(addressText,this);
    }

    @Override
    public void backwardRequested() {
        // TODO Auto-generated method stub

    }

    @Override
    public void forwardRequested() {
        // TODO Auto-generated method stub

    }

    @Override
    public void refreshRequested() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stopRequested() {
        // TODO Auto-generated method stub

    }

    @Override
    public void pageLoaded(GopherPage result) {
        System.out.println("Got gopher page!");
        System.out.print(result.getSourceCode());
    }

    @Override
    public void pageLoadFailed(GopherError error) {
        // TODO Auto-generated method stub

    }
}