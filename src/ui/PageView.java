package ui;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.GopherItem;
import net.GopherPage;
import net.GopherItem.GopherItemType;
import ui.event.NavigationInputListener;

/**
 * The PageView component renders GopherPage objects
 */
public class PageView extends JScrollPane{
    /* constants */
    private static final long serialVersionUID = 1L;

    /* local variables and objects */
    private JEditorPane viewPane;
    private JEditorPane headerPane;
    private HTMLEditorKit editorKit;
    private StyleSheet styleSheet;
    private Font textFont;
    private String viewTextColor = "#ffffff";
    private String selectionColor = "#cf9a0c";

    /* listeners for local events */
    private ArrayList<NavigationInputListener> inputListenerList;

    /* current page displayed */
    private GopherPage currentPage = null;

    /**
     * Adds a new navigation listener for any navigation events
     * @param listener
     * The listener that responds to navigation events
     */
    public void addListener(NavigationInputListener listener){
        this.inputListenerList.add(listener);
    }

    /**
     * Instead of displaying a gopher page, it displays content
     * as plain text in the view page section of this component
     * 
     * @param content
     * GopherPage with respective content
     */
    public void showGopherContent(GopherPage content){
        /* reset the header to just show nothing */
        this.headerPane.setText("");

        /* check the type of content supplied */
        if(content.getContentType() == GopherItemType.IMAGE_FILE
            || content.getContentType() == GopherItemType.GIF_FILE){
            /* try to display as an image */
            try{
                /* try to identify the file extension */
                String imageFileExt = ".jpg";
                if(content.getContentType() == GopherItemType.GIF_FILE){
                    imageFileExt = ".gif";
                }

                /* try to determine the filetype from the url */
                String imageUrl = content.getUrl().getUrlString();
                if(imageUrl.substring(imageUrl.length()-4).equals(".")){
                    imageFileExt = imageUrl.substring(imageUrl.length()-3);
                }if(imageUrl.substring(imageUrl.length()-5).equals(".")){
                    imageFileExt = imageUrl.substring(imageUrl.length()-4);
                }

                /* write the image content to file */
                File tempImageFile = File.createTempFile("gopherimagefile", imageFileExt);
                FileOutputStream outputStream = new FileOutputStream(tempImageFile);
                outputStream.write(content.getByteArray()); 
                outputStream.close();
    
                /* display content as an image */
                this.viewPane.setContentType("text/html");
                this.viewPane.setText("<img src=\"" + tempImageFile.toURI().toURL().toExternalForm() + "\" />");
            }catch(Exception ex){
                /* display exception cause as text inside the view */
                this.viewPane.setContentType("text/plain");
                this.viewPane.setText("Failed to display the image:\n" + ex.getMessage());
            }
        }else{
            /* display content as plain text */
            this.viewPane.setContentType("text/plain");
            this.viewPane.setText(content.getSourceCode().replace("\n.\r\n", ""));
        }
    }

    /**
     * Initialises rendering of a GopherPage on this view
     * 
     * @param page
     * The GopherPage to display on this view
     */
    public void showGopherPage(GopherPage page){
        /* set the current local gopher page */
        this.currentPage = page;

        /* create the headers */
        String renderedHeader = "<table cellspacing=\"0\" cellpadding=\"2\">";
        String renderedContent = "<table cellspacing=\"0\" cellpadding=\"2\">";

        int lineNumber = 1;
        for(GopherItem item : page.getItemList()){
            /* set the content for the row header */
            renderedHeader += "<tr><td class=\"lineNumber\">" + lineNumber + "</td>"
                           + "<td><div class=\"itemIcon\">" 
                           + this.getGopherItemTypeIcon(item.getItemTypeCode()) 
                           + "</div></td></tr>";

            /* set the content for the text view */
            String itemTitle = item.getUserDisplayString().replace(" ", "&nbsp;");
            String itemCode = "<span class=\"text\">" + itemTitle + "</span>";

            /* build links for anything other than infromation items */
            if(!item.getItemTypeCode().equals("i")){ 
                String linkUrlValue = item.getUrlString();
                if(linkUrlValue.startsWith("gopher://")){
                    /* create a fake http URL to represent gopher protocol as
                        otherwise the underlying Java classes will struggle
                        dealing with gopher as a protocol */
                    linkUrlValue = "http://gopher/" + linkUrlValue.substring(9);
                }

                /* create the link for this item */
                itemCode = "<a href=\"" + linkUrlValue + "\">" + itemTitle + "</a>";
            }

            /* create the item table row */
            renderedContent += "<tr><td class=\"item\">" + itemCode + "</td></tr>";

            lineNumber++;
        }

        /* set content type and add content to view */
        this.viewPane.setContentType("text/html");
        this.viewPane.setText(renderedContent+"</table>");

        /* set content type and add content to header */
        this.headerPane.setContentType("text/html");
        this.headerPane.setText(renderedHeader+"</table>");

        /* scroll the view pane to the top */
        this.viewPane.setCaretPosition(0);
    }

    /**
     * Configures the style of the view
     */
    private void configureStyle(){
        this.styleSheet = this.editorKit.getStyleSheet();
        this.styleSheet.addRule(".text { cursor:text; }");
        this.styleSheet.addRule(".lineNumber { color: #454545; }");
        this.styleSheet.addRule(".itemIcon { font-family:Feather; font-size:10px; margin-left:5px; }"); 
        this.styleSheet.addRule("a { text-decoration: none; color: #22c75c; }");  
        this.styleSheet.addRule(".item { color: " + this.viewTextColor + "; }");  
    }

    /**
     * Constructs the PageView component object 
     * 
     * @param textColor
     * The color of the text to display
     * 
     * @param backgroundColor
     * The background color of the viewer
     * 
     */
    public PageView(String textColor, String backgroundColor){
        /* instanciate input listener list */
        this.inputListenerList = new ArrayList<NavigationInputListener>();

        /* create the editor kit instance */
        this.editorKit = new HTMLEditorKit();

        /* create the editor pane */
        this.viewPane = new JEditorPane();
        this.viewPane.setEditable(false);
        this.viewPane.setBackground(Color.decode(backgroundColor));
        this.viewPane.setForeground(Color.decode(textColor));
        this.viewPane.setBorder(new EmptyBorder(10,4,8,16));
        this.viewPane.setEditorKit(this.editorKit);
        this.viewPane.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        this.viewPane.setSelectionColor(Color.decode(this.selectionColor));
        this.viewPane.setDragEnabled(false);
        this.getViewport().add(this.viewPane);

        /* set the text color locally */
        this.viewTextColor = textColor;

        /* adjust the scrollbars */
        this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        /* create the header pane with line numbers and icons */
        this.headerPane = new JEditorPane();
        this.headerPane.setEditable(false);
        this.headerPane.setBackground(Color.decode(backgroundColor));
        this.headerPane.setForeground(Color.decode(textColor));
        this.headerPane.setBorder(new EmptyBorder(10,12,8,2));
        this.headerPane.setEditorKit(this.editorKit);
        this.headerPane.setDragEnabled(false);
        this.setRowHeaderView(this.headerPane);

        /* configure the style of the header and the view */
        this.configureStyle();

        /* report any links hits as address request to the listeners */
        this.viewPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String urlValue = e.getURL().toString();
                    if(urlValue.startsWith("http://gopher/")){
                        /* revert the gopher fake url */
                        urlValue = "gopher://" + urlValue.substring(14);
                    }

                    /* determine the content type of the link target */
                    GopherItemType contentType = GopherItemType.UNKNOWN;
                    if(currentPage != null){
                        /* determine the content type of the gopher item
                            by the definition of it in the gopher menu */
                        for(GopherItem contentItem : currentPage.getItemList()){
                            if(contentItem.getUrlString().equals(urlValue)){
                                contentType = contentItem.getItemType();
                            }
                        }
                    }

                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.addressRequested(urlValue,contentType);
                    }
                }
            }
        });

        try{
            /* try to open the font for icon display */
            this.textFont = Font.createFont(Font.TRUETYPE_FONT, new File("../resources/Fonts/Inconsolata-Regular.ttf")).deriveFont(17f);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(this.textFont);

            /* apply the font settings to the view pane */
            this.viewPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            this.viewPane.setFont(this.textFont);

            /* apply the font settings to the header pane */
            this.headerPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            this.headerPane.setFont(this.textFont);
            this.headerPane.setHighlighter(null);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the fonts!");
        }
    }

    public String getGopherItemTypeIcon(String code){
        String result = "";

        if(code.equals("0")){ result = ""; }
        if(code.equals("1")){ result =  ""; } 
        if(code.equals("2")){ result =  ""; } 
        if(code.equals("3")){ result =  ""; } 
        if(code.equals("4")){ result =  "";} 
        if(code.equals("5")){ result =  ""; } 
        if(code.equals("6")){ result =  ""; } 
        if(code.equals("7")){ result =  ""; } 
        if(code.equals("8")){ result =  ""; } 
        if(code.equals("9")){ result =  ""; } 
        if(code.equals("+")){ result =  ""; } 
        if(code.equals("g")){ result =  ""; } 
        if(code.equals("I")){ result =  ""; } 
        if(code.equals("T")){ result =  ""; } 
        if(code.equals("h")){ result =  ""; }         
        if(code.equals("i")){ result =  ""; } 
        if(code.equals("s")){ result =  ""; } 
        if(code.equals("?")){ result =  ""; } 

        return result;
    }
}