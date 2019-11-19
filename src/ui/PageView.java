package ui;

import java.awt.*;
import javax.swing.*;

public class PageView extends JScrollPane{
    /* constants */
    private static final long serialVersionUID = 1L;

    /* local variables and objects */
    JEditorPane viewPane;

    /* 
        constructs the ui for this page view 
    */
    public PageView(String backgroundColor){
        /* create the editor pane */
        this.viewPane = new JEditorPane();
        this.viewPane.setEditable(false);
        this.viewPane.setBackground(Color.decode(backgroundColor));
        this.getViewport().add(this.viewPane);
        this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }
}