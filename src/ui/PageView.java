package ui;

import java.awt.*;
import java.io.File;
import javax.swing.*;

import net.GopherPage;

public class PageView extends JScrollPane{
    /* constants */
    private static final long serialVersionUID = 1L;

    /* local variables and objects */
    JEditorPane viewPane;
    Font textFont;

    public void showGopherPage(GopherPage page){
        this.viewPane.setText(page.getSourceCode());
    }

    /* 
        constructs the ui for this page view 
    */
    public PageView(String textColor, String backgroundColor){
        /* create the editor pane */
        this.viewPane = new JEditorPane();
        this.viewPane.setEditable(false);
        this.viewPane.setBackground(Color.decode(backgroundColor));
        this.viewPane.setForeground(Color.decode(textColor));
        this.getViewport().add(this.viewPane);
        this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.getVerticalScrollBar().setOpaque(false);
        
        try{
            /* try to open the font for icon display */
            this.textFont = Font.createFont(Font.TRUETYPE_FONT, new File("../resources/Fonts/Inconsolata-Regular.ttf")).deriveFont(17f);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(this.textFont);
            this.viewPane.setFont(this.textFont);
        }catch(Exception ex){
            /* Ouchie, this will look bad... */
            System.out.println("Unable to load the fonts!");
        }
    }
}