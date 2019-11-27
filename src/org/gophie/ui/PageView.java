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
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.gophie.config.*;
import org.gophie.net.GopherItem;
import org.gophie.net.GopherPage;
import org.gophie.net.GopherItem.GopherItemType;
import org.gophie.ui.event.NavigationInputListener;
import org.gophie.ui.event.PageMenuEventListener;

/**
 * The PageView component renders GopherPage objects
 */
public class PageView extends JScrollPane{
    /* constants */
    private static final long serialVersionUID = 1L;

    /* local variables and objects */
    private PageMenu pageMenu;
    private JEditorPane viewPane;
    private JEditorPane headerPane;
    private HTMLEditorKit editorKit;
    private StyleSheet styleSheet;
    private Font textFont;
    private String viewTextColor = "#ffffff";
    private String selectionColor = "#cf9a0c";

    /* the config file with all settings */
    private ConfigFile configFile;

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

        /* set current page to the page menu */
        this.pageMenu.setCurrentPage(content);

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

                /* determine image size and rescale */
                String imageHtmlCode = "<img src=\"" + tempImageFile.toURI()
                                        .toURL().toExternalForm() + "\" />";
                
                try{
                    BufferedImage bufferedImage = ImageIO.read(tempImageFile);
                    int width = bufferedImage.getWidth();
                    int height = bufferedImage.getHeight();
                    if(width > 800){
                        height = (height / (width / 800));
                        imageHtmlCode = "<img src=\"" + tempImageFile.toURI()
                                        .toURL().toExternalForm() + "\" "
                                        + "width=\"800\" height=" + height + "\""
                                        + "/>";
                    }
                }catch(Exception ex){
                    /* failed to determine image size */
                    System.out.println("Failed to determine image size: " + ex.getMessage());
                }
    
                /* display content as an image */
                this.viewPane.setContentType("text/html");
                this.viewPane.setText(imageHtmlCode);
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

        /* set current page to the page menu */
        this.pageMenu.setCurrentPage(page);

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
                /* create the link for this item */
                itemCode = "<a href=\"" + item.getUrlString() + "\">" + itemTitle + "</a>";
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
        /* get the color schemes from the config file */
        String linkColor = this.configFile.getSetting("PAGE_LINK_COLOR", "Appearance", "#22c75c");
        String lineNumberColor = this.configFile.getSetting("PAGE_LINENUMBER_COLOR", "Appearance", "#454545");

        /* build up the stylesheet for the rendering */
        this.styleSheet = this.editorKit.getStyleSheet();
        this.styleSheet.addRule("body { white-space:nowrap; }");
        this.styleSheet.addRule(".text { cursor:text; }");
        this.styleSheet.addRule(".lineNumber { color: " + lineNumberColor + "; }");
        this.styleSheet.addRule(".itemIcon { font-family:Feather; font-size:10px; margin-left:5px; }"); 
        this.styleSheet.addRule("a { text-decoration: none; color: " + linkColor + "; }");  
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
    public PageView(MainWindow parent, String textColor, String backgroundColor){
        /* get the config file to fetch the settings */
        this.configFile = ConfigurationManager.getConfigFile();

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
        this.viewPane.setSelectionColor(Color.decode(this.configFile.getSetting
                    ("PAGE_SELECTION_COLOR", "Appearance", this.selectionColor)));

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

        /* create the page menu and attach the popup trigger */
        this.pageMenu = new PageMenu();
        this.pageMenu.addPageMenuEventListener((PageMenuEventListener)parent);
        this.viewPane.add(this.pageMenu);
        this.viewPane.addMouseListener(new MouseAdapter(){
            /* handle the popup trigger for this document */
            public void mouseReleased(MouseEvent evt){
                /* get the trigger button for the menu from config
                    (right mouse button id is usually #3) */
                int menuTriggerButtonId = Integer.parseInt(configFile.getSetting
                                ("MENU_MOUSE_TRIGGERBUTTON", "Navigation", "3"));
                if(evt.getButton() == menuTriggerButtonId){
                    /* trigger hit, show the page menu and also
                        make sure to pass the text selection before */
                    pageMenu.setSelectedText(viewPane.getSelectedText());

                    /* show the menu */
                    pageMenu.show(viewPane, 
                            (int)evt.getPoint().getX(), 
                            (int)evt.getPoint().getY());
                }
            }
        });

        /* report any links hits as address request to the listeners */
        this.viewPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                /* get the url of that link */
                String urlValue = e.getDescription();

                /* determine the content type of the link target */
                GopherItem itemObject = null;
                if(currentPage != null){
                    /* determine the content type of the gopher item
                        by the definition of it in the gopher menu */
                    for(GopherItem contentItem : currentPage.getItemList()){
                        if(contentItem.getUrlString().equals(urlValue)){
                            itemObject = contentItem;
                        }
                    }
                }

                /* pass the active link item to the popup menu */
                if(e.getEventType() == HyperlinkEvent.EventType.ENTERED){
                    pageMenu.setLinkTarget(itemObject);
                }

                /* reset the link target for the popup menu */
                if(e.getEventType() == HyperlinkEvent.EventType.EXITED){
                    pageMenu.setLinkTarget(null);
                }

                /* handle link activation (aka left-click) */
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    /* execute the handler */
                    for (NavigationInputListener inputListener : inputListenerList){
                        inputListener.addressRequested(urlValue,itemObject);
                    }
                }
            }
        });

        /* try to open the font for icon display */
        this.textFont = ConfigurationManager.getConsoleFont(17f);

        /* apply the font settings to the view pane */
        this.viewPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        this.viewPane.setFont(this.textFont);

        /* apply the font settings to the header pane */
        this.headerPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        this.headerPane.setFont(this.textFont);
        this.headerPane.setHighlighter(null);
    }

    /**
     * Selects all the items in the view
     */
    public void selectAllText(){
        /* just pass it onto the view */
        this.viewPane.selectAll();
        this.viewPane.requestFocus();
    }

    /**
     * Returns the header icon for the gopher item type
     * 
     * @param code
     * Code for the gopher item type
     * 
     * @return
     * String with the icon for the item
     */
    public String getGopherItemTypeIcon(String code){
        String result = "";

        if(code.equals("0")){ result = ""; }
        if(code.equals("1")){ result =  ""; } 
        if(code.equals("2")){ result =  ""; } 
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