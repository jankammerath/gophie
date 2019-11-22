package ui;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.*;

/* import gopher network client */
import net.GopherClient;
import net.GopherPage;
import net.GopherUrl;
import net.GopherItem.GopherItemType;
import net.event.GopherClientEventListener;
import net.event.GopherError;
import ui.event.MessageViewListener;
/* import ui event listeners */
import ui.event.NavigationInputListener;

public class MainWindow implements NavigationInputListener, GopherClientEventListener {
    /* define the constants for the UI */
    public static final String APPLICATION_TITLE = "Gophie";
    public static final String NAVIGATIONBAR_BACKGROUND = "#248AC2";
    public static final String NAVIGATIONBAR_TEXTCOLOR = "#76bce3";
    public static final String NAVIGATIONBAR_TEXTHOVERCOLOR = "#ffffff";
    public static final String VIEW_BACKGROUND = "#1b1b1b";
    public static final String VIEW_TEXTCOLOR = "#e8e8e8";
    public static final String DEFAULT_GOPHERHOME = "gopher.floodgap.com";

    /* local network objects */
    private GopherClient gopherClient;

    /* storage with history for browsing */
    private ArrayList<GopherPage> history = new ArrayList<GopherPage>();
    private int historyPosition = -1;

    /* local ui elements */
    private JFrame frame;
    private PageView pageView;
    private NavigationBar navigationBar;
    private MessageView messageView;

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
        this.pageView = new PageView(VIEW_TEXTCOLOR, VIEW_BACKGROUND);
        this.pageView.addListener(this);

        /* create the menu bar */
        this.frame.setJMenuBar(new MainMenu());

        /* create the navigation bar */
        this.navigationBar = new NavigationBar(NAVIGATIONBAR_BACKGROUND, NAVIGATIONBAR_TEXTCOLOR,
                NAVIGATIONBAR_TEXTHOVERCOLOR);
        this.navigationBar.setAddressText(DEFAULT_GOPHERHOME);

        /* attach listener to navigation bar */
        this.navigationBar.addListener(this);

        /* create the message view */
        this.messageView = new MessageView();

        /* set the content pane */
        Container contentPane = frame.getContentPane();
        contentPane.add(this.messageView, BorderLayout.NORTH);
        contentPane.add(this.pageView, BorderLayout.CENTER);
        contentPane.add(this.navigationBar, BorderLayout.SOUTH);
        this.frame.setVisible(true);

        /* fetch the default gopher home */
        this.addressRequested(DEFAULT_GOPHERHOME, GopherItemType.GOPHERMENU);
    }

    public void show() {
        /* display the window */
        this.frame.pack();
        this.frame.setVisible(true);
    }

    /**
     * Updates the history with a new page
     * 
     * @param page
     * The page that was received
     */
    private void updateHistory(GopherPage page){
        Boolean addToHistory = false;

        /* check if current position is at last page */
        if(this.historyPosition == this.history.size()-1){
            /* add this page to the history */
            if(this.history.size() > 0){
                /* make sure this was not just a reload and the last
                    page in the history is not already ours */
                if(!this.history.get(this.history.size()-1).getUrl().getUrlString()
                                        .equals(page.getUrl().getUrlString())){
                    /* just drop it in */
                    addToHistory = true;
                }
            }else{
                /* empty history, just drop in the page */
                addToHistory = true;
            }    
        }else{
            /* user navigation inside history, check if the current
                page is at the position in history or if it is a 
                new page the user went to */
            if(!this.history.get(this.historyPosition).getUrl()
                .getUrlString().equals(page.getUrl().getUrlString())){
                /* it is a new page outside the history, keep the history
                    up until the current page and add this page as a new
                    branch to the history, eliminating the 
                    previous branch forward */
                ArrayList<GopherPage> updatedHistory = new ArrayList<GopherPage>();
                for(int h=0; h<=this.historyPosition; h++){
                    updatedHistory.add(this.history.get(h));
                }

                /* update the history */
                this.history = updatedHistory;

                /* allow adding to history */
                addToHistory = true;
            }
        }    

        /* reset navigation allowance */
        this.navigationBar.setNavigateBack(false);
        this.navigationBar.setNavigateForward(false);

        /* add to history, if allowed */
        if(addToHistory == true){
            /* add to the stack of pages */
            this.history.add(page);

            /* update position to the top */
            this.historyPosition = this.history.size()-1;

            /* disable forward */
            this.navigationBar.setNavigateForward(false);
            if(this.history.size() > 1){
                /* allow back if more than just this page exist */
                this.navigationBar.setNavigateBack(true);
            }
        }else{
            /* if position is 0, there is nowhere to go back to */
            if(this.historyPosition > 0){
                /* allow navigation back in history */
                this.navigationBar.setNavigateBack(true);
            }if(this.historyPosition < (this.history.size()-1)){
                /* if position is at the end, there is nowhere
                    to move forward to */
                this.navigationBar.setNavigateForward(true);
            }
        }
    }

    /**
     * Process a request to go to an address or URL
     * 
     * @param addressText
     * The text or URL of the address, the client will guess the correct URL
     * 
     * @param contentType
     * The expected content type of the content behind the address
     */
    @Override
    public void addressRequested(String addressText, GopherItemType contentType) {
        /* check what type of link was requested and execute
            the appropriate external application or use the
            default approach for gopher content */
        if(addressText.startsWith("https://") == true 
            || addressText.startsWith("http://") == true){
            /* this is the World Wide Web using HTTP or HTTPS, 
                so try to open the systems browser so that the
                user can enjoy bloated javascript based html
                content with the fine-art of pop-up advertising
                and animated display banners */
            String confirmText = "Open \"" + addressText + "\" with your web browser?";
            String[] optionList = new String[2];
            optionList[0] = "Open Website";
            optionList[1] = "Dismiss";
            this.messageView.showConfirm(confirmText, optionList, new MessageViewListener(){
                @Override
                public void optionSelected(int option) {
                    if(option == 0){
                        /* launch the system WWW browser */
                        if (Desktop.isDesktopSupported() == true 
                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            try{
                                /* launch the systems WWW browser */
                                Desktop.getDesktop().browse(new URI(addressText));
                            }catch(Exception ex){
                                /* Error: cannot enjoy bloated javascript 
                                        stuffed World Wide Web pages! */
                                System.out.println("Unable to open system's "
                                    + "world wide web browser: " + ex.getMessage());
                            }
                        }
                        /* hide the message view */
                        messageView.setVisible(false);
                    }else{
                        /* hide the message view */
                        messageView.setVisible(false);
                    }
                }
            });
        }else{
            /* this is default gopher content */
            /* activate the load indicator in the address bar */
            this.navigationBar.setIsLoading(true);

            /* update the navigation bar with the new address */
            this.navigationBar.setAddressText(addressText);

            try{
                /* try to execute the thread */
                this.gopherClient.fetchAsync(addressText,contentType,this);
            }catch(Exception ex){
                /* might throw an ex when thread is interrupted */
                System.out.println("Exception while fetching async: " + ex.getMessage());
            }
        }
    }

    @Override
    public void backwardRequested() {
        /* set the new history position */
        if(this.historyPosition > 0){
            this.historyPosition--;

            /* get the new page from history */
            this.pageLoaded(this.history.get(this.historyPosition));

            /* update the history */
            this.updateHistory(this.history.get(this.historyPosition));
        }
    }

    @Override
    public void forwardRequested() {
        /* set the new history position */
        if(this.historyPosition < (this.history.size()-1)){
            this.historyPosition++;

            /* get the new page from history */
            this.pageLoaded(this.history.get(this.historyPosition));

            /* update the history */
            this.updateHistory(this.history.get(this.historyPosition));
        }
    }

    @Override
    public void refreshRequested() {
        /* get the current gopher page to reload it */
        GopherPage currentPage = this.history.get(this.historyPosition);

        /* reload practically means just requesting this page again */
        this.addressRequested(currentPage.getUrl().getUrlString(), currentPage.getContentType());
    }

    @Override
    public void stopRequested() {
        /* cancel any current operation */
        this.gopherClient.cancelFetch();

        /* notify the local handler about cancellation by the user */
        this.pageLoadFailed(GopherError.USER_CANCELLED,null);
    }

    /**
     * Handles page load events from the listener
     * 
     * @param result
     * The gopher page that was received
     */
    @Override
    public void pageLoaded(GopherPage result) {
        /* set the window title to the url of this page */
        this.frame.setTitle(result.getUrl().getUrlString() 
                            + " - " + APPLICATION_TITLE);

        /* update the address text with the loaded page */
        this.navigationBar.setAddressText(result.getUrl().getUrlString());

        /* detect the content type and determine how the handle it */
        if(result.getContentType() == GopherItemType.GOPHERMENU){
            /* this is a gopher menu hence it is rendered like
                one including highlighting of links and 
                the menu icons for the various item types */
            this.pageView.showGopherPage(result);
        }else{
            /* this is plain content, so render it
                appropriately and let the view decide
                on how to handle the content */
            this.pageView.showGopherContent(result);
        }

        /* update the history */
        this.updateHistory(result);

        /* reset the loading indicators */
        this.navigationBar.setIsLoading(false);
    }

    @Override
    public void pageLoadFailed(GopherError error, GopherUrl url) {
        /* show message for connection timeout */
        if(error == GopherError.CONNECTION_TIMEOUT){
            if(url != null){
                this.messageView.showInfo("Connection timed out: " + url.getHost());
            }
        }

        /* show DNS or host not found error */
        if(error == GopherError.HOST_UNKNOWN){
            if(url != null){
                this.messageView.showInfo("Server not found: " + url.getHost());
            }
        }

        /* show some information about an exception */
        if(error == GopherError.EXCEPTION){
            this.messageView.showInfo("Ouchn, an unknown error occured.");
        }

        /* output some base information to the console */
        System.out.println("Failed to load gopher page: " + error.toString());

        /* reset the navigation bar status */
        this.navigationBar.setIsLoading(false);
    }
}