/*
    This file is part of Gophie.

    Gophie is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gophie is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gophie. If not, see <https://www.gnu.org/licenses/>.

*/

package org.gophie.ui;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;

import javax.swing.*;

/* import application config classes */
import org.gophie.config.ConfigFile;
import org.gophie.config.ConfigurationManager;
import org.gophie.config.SystemUtility;

/* import gopher network client */
import org.gophie.net.*;
import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.net.event.*;

/* import ui event listeners */
import org.gophie.ui.event.*;

public class MainWindow implements NavigationInputListener, GopherClientEventListener, PageMenuEventListener {
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
    private DownloadList downloadList;

    /* storage with history for browsing */
    private ArrayList<GopherPage> history = new ArrayList<GopherPage>();
    private int historyPosition = -1;

    /* local ui elements */
    private JFrame frame;
    private PageView pageView;
    private NavigationBar navigationBar;
    private JPanel headerBar;
    private MessageView messageView;
    private SearchInput searchInput;
    private DownloadWindow downloadWindow;

    private String address = "";
    private boolean openTextInNewWindow = true;
    private boolean openDirInNewWindow = false;


    /**
     * Locate the window near to the mouse pointer.
     */
    public static Point locateWindow(int width, int height) {
        long    rx, ry;
        Point    p;

        try { p = MouseInfo.getPointerInfo().getLocation(); }
	catch (Exception e) { p = new Point(10, 20); }

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        rx = Math.round( Math.random() * dim.width / 5 - dim.width / 10);
        ry = Math.round( Math.random() * dim.height / 5 - dim.height / 10);

        if (p.x > dim.width / 2) { p.x -= width + 20 - rx; }
        else p.x -= 20 + rx;

        if (p.x < 0) p.x = 0;

        if (p.y > dim.height / 2) { p.y -= height + 20 - ry; }
        else { p.y -= 20 + ry; }

        if (p.y < 0) p.y = 0;

        return (p);
    }


    /**
     * Constructs the main window and make it ready for display.
     */
    private void init() {
        /* get the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();

        /* create the instance of the client */
        this.gopherClient = new GopherClient();

        /* create the download list */
        this.downloadList = new DownloadList();

        /* create the download window */
        this.downloadWindow = new DownloadWindow(this.downloadList);

        /* create the main window */
        this.frame = new JFrame(APPLICATION_TITLE);
        this.frame.setMinimumSize(new Dimension(800, 600));
        // this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.setIconImage(ConfigurationManager.getImage("icon.png"));

        /* create the page view component object */
        this.pageView = new PageView(this,
            configFile.getSetting("VIEW_TEXTCOLOR", "Appearance", VIEW_TEXTCOLOR), 
            configFile.getSetting("VIEW_BACKGROUND", "Appearance", VIEW_BACKGROUND));
        this.pageView.addListener(this);

        /* create the navigation bar */
        this.navigationBar = new NavigationBar(
            /* get the appearance configuration from the config file */
            configFile.getSetting("NAVIGATIONBAR_BACKGROUND", "Appearance", NAVIGATIONBAR_BACKGROUND), 
            configFile.getSetting("NAVIGATIONBAR_TEXTCOLOR", "Appearance", NAVIGATIONBAR_TEXTCOLOR),
            configFile.getSetting("NAVIGATIONBAR_TEXTHOVERCOLOR", "Appearance", NAVIGATIONBAR_TEXTHOVERCOLOR)
        );
        
        /* set the gopher home as defined in the config
            or use the default one if none is defined */
        String address = configFile.getSetting("GOPHERHOME", "Navigation", DEFAULT_GOPHERHOME);
        this.navigationBar.setAddressText(address);

        /* Get the flags who items are openend. */
        String f = configFile.getSetting("item-in-new-window", "Navigation", "true");
	openTextInNewWindow = (f.equals("yes")  ||  f.equals("true")  ||  f.equals("1"))? true: false;

        f = configFile.getSetting("menu-in-new-window", "Navigation", "false");
	openDirInNewWindow = (f.equals("yes")  ||  f.equals("true")  ||  f.equals("1"))? true: false;

        /* attach listener to navigation bar */
        this.navigationBar.addListener(this);

        /* create the header bar, message view
            and search input component */
        this.headerBar = new JPanel();
        this.headerBar.setLayout(new BoxLayout(this.headerBar,BoxLayout.Y_AXIS));
        this.messageView = new MessageView();
        this.headerBar.add(this.messageView);
        this.searchInput = new SearchInput();
        this.headerBar.add(this.searchInput);

        /* set the content pane */
        Container contentPane = frame.getContentPane();
        contentPane.add(this.headerBar, BorderLayout.NORTH);
        contentPane.add(this.pageView, BorderLayout.CENTER);
        contentPane.add(this.navigationBar, BorderLayout.SOUTH);

        /* start the window in the center of the screen */
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	this.frame.setLocation(locateWindow(800, 600));
        // this.frame.setLocation(dim.width/2-this.frame.getSize().width/2, 
        //                     dim.height/2-this.frame.getSize().height/2);
    }

    /**
     * Standard constructor.  Not used anymore.
     */
    public MainWindow() {
    	this.init();

	/* display the window */
        this.frame.setVisible(true);

        /* fetch the default gopher home */
        this.fetchGopherContent(address, GopherItemType.GOPHERMENU);
    }

    /**
     * Create window and open a specific location if set.
     */
    public MainWindow(String addr, GopherItemType type) {
    	this.init();

	/* If a URL is given, set it. Notice that it must be a
	 * Gopher directory. */
	if (addr != null  &&  ! addr.equals(""))
		address = addr;

        this.navigationBar.setAddressText(address);
	this.frame.setVisible(true);
        this.fetchGopherContent(address, type);
    }

    /**
     * Shows this main window
     */
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
     * Prompts user to choose on how to handle the
     * file and whether it should be saved only or
     * immediately downloaded to user home and 
     * executed or opened when finished
     * 
     * @param addressText
     * the address (URL) to download
     * 
     * @param item
     * the item to download
     */
    public void confirmDownload(String addressText, GopherItem item){
        /* binary files are handled by the download manager */
        String confirmText = "Download \"" + item.getFileName() 
                    + "\" from \"" + item.getHostName() + "\"?";
        String[] optionList = new String[]{"Open", "Save", "Dismiss"};
        this.messageView.showConfirm(confirmText, optionList, new MessageViewListener(){
            @Override
            public void optionSelected(int option) {
                if(option == 0){
                    /* store file to download directory and open */
                    String targetFileName = ConfigurationManager.getDownloadPath() + item.getFileName();
                    downloadList.add(new DownloadItem(item,targetFileName,true));

                    /* hide the message view */
                    messageView.setVisible(false);
                }if(option == 1){
                    /* initiate the download */
                    initiateDownload(item);

                    /* hide the message view */
                    messageView.setVisible(false);
                }

                /* hide the message view */
                messageView.setVisible(false);
            }
        });
    }

    /**
     * Prompts user to select the file destination
     * and immediately executes the download of the
     * file
     * 
     * @param fileItem
     * the item to download
     */
    public void initiateDownload(GopherItem fileItem){
        /* let user select where to store the file */
        FileDialog fileDialog = new FileDialog(frame, "Download and save file", FileDialog.SAVE);
        fileDialog.setFile(fileItem.getFileNameWithForcedExt());
        fileDialog.setVisible(true);
        String targetFileName = fileDialog.getDirectory() + fileDialog.getFile();
        if(targetFileName.equals(null) == false
            && targetFileName.equals("nullnull") == false){
            /* pass url and target file to download manager */
            downloadList.add(new DownloadItem(fileItem,targetFileName,false));
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
    public void addressRequested(String addressText, GopherItem item, boolean shifted) {

	GopherItemType type = item.getItemType();
	boolean openInNewWindow = false;

        if (type == GopherItemType.TEXTFILE  ||  type == GopherItemType.FULLTEXT_SEARCH
	    ||  type == GopherItemType.IMAGE_FILE  ||  type == GopherItemType.GIF_FILE) {
	    if ((openTextInNewWindow  ||  shifted)  &&  ! (openTextInNewWindow  &&  shifted)) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
	    	        MainWindow win = new MainWindow(addressText, item.getItemType());
		        win.show();
		    }
		});

                return;
	    }
	} else if (item.getItemType() == GopherItemType.GOPHERMENU) {
	    if ((openDirInNewWindow  ||  shifted)  &&  ! (openDirInNewWindow  &&  shifted)) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
	    	        MainWindow win = new MainWindow(addressText, item.getItemType());
		        win.show();
		    }
		});

                return;
	    }
	} else if (item.getItemType() == GopherItemType.UNKNOWN) {
	    openInNewWindow = shifted;
	}


	if (openInNewWindow) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
	    	        MainWindow win = new MainWindow(addressText, item.getItemType());
		        win.show();
		    }
		});

                return;
	    }


        /* check if this file is binary or not as
            binaries such as media or other files
            will be handled differently (e.g. downloaded) */
        if(item.isBinaryFile()){
            /* binary files are handled by the download manager */
            this.confirmDownload(addressText,item);
        }else{
            /* this is not a binary file, try to handle and render */
            switch(item.getItemType()){
                case FULLTEXT_SEARCH:
                    /* show the search interface */
                    this.searchInput.performSearch(item.getUserDisplayString(), new SearchInputListener(){
                        @Override
                        public void searchRequested(String text) {
                            /* execute search through gopher */
                            String searchQueryText = addressText + "\t" + text;
                            fetchGopherContent(searchQueryText,GopherItemType.GOPHERMENU);               
                        }
                    });
                    break;
                case CCSCO_NAMESERVER:
                    /* CCSO is not part of the Gopher protocol, but its very own
                        protocol and apart from floodgap.com's CCSO server there
                        is hardly any server to test interaction with. The CCSO
                        protocol can also be considered quite simple. A CCSO client
                        would be a software of its own, but sources are even fewer
                        than Gopher servers out there. Hence, Gophie allows the
                        user to use CCSO servers throgh their Telnet client. */
                    this.openTelnetSession(item.getHostName(), item.getPortNumber());
                    break;
                case TELNET:
                    /* handle telnet session requests */
                    this.openTelnetSession(item.getHostName(), item.getPortNumber());
                    break;
                case TELNET3270:
                    /* handle telnet 3270 session requests */
                    this.openTelnetSession(item.getHostName(), item.getPortNumber());
                    break;
                default:
                    if (addressText.startsWith("http://") == true) {
                        this.fetchGopherContent(addressText, item.getItemType());
		    }
                    /* check what type of link was requested and execute
                        the appropriate external application or use the
                        default approach for gopher content */
                    else if(addressText.startsWith("https://") == true 
                        || addressText.startsWith("http://") == true){
                        /* this is the World Wide Web using HTTP or HTTPS, 
                            so try to open the systems browser so that the
                            user can enjoy bloated javascript based html
                            content with the fine-art of pop-up advertising
                            and animated display banners */
                        this.openWebContent(addressText,item.getItemType());
                    }else if(addressText.startsWith("mailto:") == true){
                        /* this is a mailto link */
                        this.openEmailClient(addressText.replace("mailto:", ""));
                    }else{
                        /* just fetch as regular gopher content */
                        this.fetchGopherContent((new GopherUrl(addressText)).getUrlString(),item.getItemType());
                    }
                    break;
            }
        }
    }

    /**
     * Opens the clients email address
     * 
     * @param emailAddress
     * the email address to send an email to
     */
    private void openEmailClient(String emailAddress){
        String confirmText = "Do you want to send an e-mail to \"" + emailAddress + "\"?";
        String[] optionList = new String[]{"Create new e-mail", "Dismiss"};
        this.messageView.showConfirm(confirmText, optionList, new MessageViewListener(){
            @Override
            public void optionSelected(int option) {
                if(option == 0){
                    /* launch the system email client */
                    if (Desktop.isDesktopSupported() == true 
                        && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try{
                            /* launch the mailto handler of the system */
                            Desktop.getDesktop().browse(new URI("mailto:"+emailAddress));
                        }catch(Exception ex){
                            /* Error: cannot open email client */
                            System.out.println("Unable to open system's "
                                + "email client: " + ex.getMessage());
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
    }

    /**
     * Prompts the user and opens the systems telnet client
     * 
     * @param hostName
     * host name of the telnet server
     * 
     * @param portNumber
     * port number of the telnet server
     */
    private void openTelnetSession(String hostName, int portNumber){
        String confirmText = "Open a Telnet session with \"" + hostName + ":" + portNumber + "\"?";
        String[] optionList = new String[]{"Open Telnet", "Dismiss"};
        this.messageView.showConfirm(confirmText, optionList, new MessageViewListener(){
            @Override
            public void optionSelected(int option) {
                if(option == 0){
                    /* launch the system WWW browser */
                    if (Desktop.isDesktopSupported() == true 
                        && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try{
                            /* launch the systems telnet client by creating
                                a telnet URI and calling the systems protocol handler */
                            String telnetUri = "telnet://" + hostName + ":" + portNumber;
                            Desktop.getDesktop().browse(new URI(telnetUri));
                        }catch(Exception ex){
                            /* Error: cannot open telnet client */
                            System.out.println("Unable to open system's "
                                + "telnet client: " + ex.getMessage());
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
    }

    /**
     * Ask user to open web content through http
     * 
     * @param addressText
     * The actual address requested
     * 
     * @param contentType
     * The actual content type of the content
     */
    private void openWebContent(String addressText, GopherItemType contentType){
        String confirmText = "Open \"" + addressText + "\" with your web browser?";
        String[] optionList = new String[]{"Open Website", "Dismiss"};
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
    }

    /**
     * Fetches gopher menu or text content
     * 
     * @param addressText
     * The address to fetch content from
     * 
     * @param contentType
     * The actual content type requested
     */
    private void fetchGopherContent(String addressText, GopherItemType contentType){
        /* this is default gopher content */
        /* activate the load indicator in the address bar */
        this.navigationBar.setIsLoading(true);

        /* update the navigation bar with the new address */
        String address = addressText;

        /* check if selector prefixes are enabled */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String prefixEnabled = configFile.getSetting("SELECTOR_PREFIX_ENABLED", "Navigation", "yes");
        if(prefixEnabled.equals("yes")){
            /* create the gopher url object for the address */
            GopherUrl prefixUrl = new GopherUrl(addressText);
            prefixUrl.setTypePrefix(GopherItem.getTypeCode(contentType));

            /* set the address to the url with the prefix */
            address = prefixUrl.getUrlString(true);
        }

        /* update the navigation bar with the new address */
        this.navigationBar.setAddressText(address);

        try{
            /* try to execute the thread */
            this.gopherClient.fetchAsync(addressText,contentType,this);
        }catch(Exception ex){
            /* might throw an ex when thread is interrupted */
            System.out.println("Exception while fetching async: " + ex.getMessage());
        }        
    }

    /**
     * Navigates backwards in the history
     */
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

    /**
     * Refreshes the current page
     */
    @Override
    public void refreshRequested() {
        /* get the current gopher page to reload it */
        GopherPage currentPage = this.history.get(this.historyPosition);

        /* reload practically means just requesting this page again */
        this.fetchGopherContent(currentPage.getUrl().getUrlString(), currentPage.getContentType());
    }

    /**
     * Stops the current page load
     */
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
            + " (" + SystemUtility.getFileSizeString(result.getByteArray().length) + ")"
            + " - " + APPLICATION_TITLE);

        /* update the address text with the loaded page */
        String address = result.getUrl().getUrlString();

        /* check if selector prefixes are enabled */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String prefixEnabled = configFile.getSetting("SELECTOR_PREFIX_ENABLED", "Navigation", "yes");
        if(prefixEnabled.equals("yes")){
            /* create the gopher url object for the address */
            GopherUrl prefixUrl = result.getUrl();
            prefixUrl.setTypePrefix(GopherItem.getTypeCode(result.getContentType()));

            /* set the address to the url with the prefix */
            address = prefixUrl.getUrlString(true);
        }

        /* set the navigation bar to the new address */
        this.navigationBar.setAddressText(address);


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

    /**
     * Reports failed page load
     */
    @Override
    public void pageLoadFailed(GopherError error, GopherUrl url) {
        /* show message for connection timeout */
        if(error == GopherError.CONNECT_FAILED){
            if(url != null){
                this.messageView.showInfo("Connection refused: " + url.getHost());
            }
        }

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

    /**
     * Report progress on the page loading
     */
    @Override
    public void progress(GopherUrl url, long byteCount) {
        /* report the download size in the title bar */
        this.frame.setTitle(url.getUrlString() 
            + " (" + SystemUtility.getFileSizeString(byteCount) + ")"
            + " - " + APPLICATION_TITLE);
    }

    /**
     * Toggles the download window
     */
    @Override
    public void showDownloadRequested() {
        if(this.downloadWindow.isVisible()){
            this.downloadWindow.hide();
        }else{
            this.downloadWindow.show(this.frame);
        }
    }

    /**
     * Updates the gopher home with the provided url
     */
    @Override
    public void setHomeGopherRequested(String url) {
        /* set the gopher home to the config file */
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        configFile.setSetting("GOPHERHOME", url, "Navigation");
        configFile.save();
    }

    /**
     * initiates the download of the requested file
     */
    @Override
    public void itemDownloadRequested(GopherItem item) {
        this.initiateDownload(item);
    }

    /**
     * Saves the current page to file
     */
    @Override
    public void pageSaveRequested(GopherPage page) {
        /* let user select where to store the file */
        FileDialog fileDialog = new FileDialog(frame, "Save current file", FileDialog.SAVE);
        fileDialog.setFile(page.getFileName());
        fileDialog.setVisible(true);
        String targetFileName = fileDialog.getDirectory() + fileDialog.getFile();
        if(targetFileName.equals(null) == false
            && targetFileName.equals("nullnull") == false){
            /* pass url and target file to download manager */
            page.saveAsFile(targetFileName);
        } 
    }

    /**
     * Loads the item from the selected text.
     */
    @Override
    public void getAndGoRequested(GopherPage page, String selected, boolean shifted) {
        GopherUrl url = page.getUrl();
	String address = url.makeUrlString(selected);
	GopherUrl u2 = new GopherUrl(address);

	GopherItem item = new GopherItem(GopherItemType.UNKNOWN, u2);
	item.guessItemType();

        addressRequested(address, item, shifted);
    }

    /**
     * Close the window.
     */
    public void closeRequested() {
        this.frame.dispose();
    }

    /**
     * Exits the program.
     */
    public void exitRequested() {
        System.exit (0);
    }

    /**
     * Selects all text on the current page
     */
    @Override
    public void selectAllTextRequested() {
        /* hand that one back to the page view */
        this.pageView.selectAllText();
    }

    /**
     * Sends the user to his gopher home
     */
    @Override
    public void homeGopherRequested() {
        ConfigFile configFile = ConfigurationManager.getConfigFile();
        String homeGopherUrl = configFile.getSetting("GOPHERHOME", "Navigation", DEFAULT_GOPHERHOME);
        this.fetchGopherContent(homeGopherUrl, GopherItemType.GOPHERMENU);
        this.navigationBar.setAddressText(homeGopherUrl);
    }

    /**
     * Handles item mismatch events when the content detection
     * during the page loading process detects a different file
     * type than the one requested.
     */
    @Override
    public void pageLoadItemMismatch(GopherItemType requested, GopherItemType detected, GopherUrl url) {
        /* reset the navigation bar status */
        this.navigationBar.setIsLoading(false);

        /* binary files are handled by the download manager */
        this.confirmDownload(url.getUrlString(),(new GopherItem(detected,url)));
    }
}
