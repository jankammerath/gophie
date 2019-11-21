package ui;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.GopherItem;
import net.GopherPage;
import ui.event.NavigationInputListener;

/**
 * The PageView component renders GopherPage objects
 */
public class PageView extends JScrollPane{
    /* constants */
    private static final long serialVersionUID = 1L;

    /* local variables and objects */
    JEditorPane viewPane;
    JEditorPane headerPane;
    HTMLEditorKit editorKit;
    Font textFont;
    String viewTextColor = "#ffffff";

    /* listeners for local events */
    private ArrayList<NavigationInputListener> inputListenerList;

    /**
     * Adds a new navigation listener for any navigation events
     * @param listener
     * The listener that responds to navigation events
     */
    public void addListener(NavigationInputListener listener){
        this.inputListenerList.add(listener);
    }

    /**
     * Initialises rendering of a GopherPage on this view
     * 
     * @param page
     * The GopherPage to display on this view
     */
    public void showGopherPage(GopherPage page){
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
            String itemCode = itemTitle;

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

        this.viewPane.setText(renderedContent+"</table>");
        this.headerPane.setText(renderedHeader+"</table>");
    }

    /**
     * Configures the style of the view
     */
    private void configureStyle(){
        StyleSheet styleSheet = this.editorKit.getStyleSheet();
        styleSheet.addRule(".lineNumber { color: #454545; }");
        styleSheet.addRule(".itemIcon { font-family:Feather; font-size:10px; margin-left:5px; }"); 
        styleSheet.addRule("a { text-decoration: none; color: #22c75c; }");  
        styleSheet.addRule(".item { color: " + this.viewTextColor + "; }");      
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
        this.viewPane.setBorder(new EmptyBorder(10,4,8,8));
        this.viewPane.setContentType("text/html");
        this.viewPane.setEditorKit(this.editorKit);
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
        this.headerPane.setContentType("text/html");
        this.headerPane.setEditorKit(this.editorKit);
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

                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.addressRequested(urlValue);
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