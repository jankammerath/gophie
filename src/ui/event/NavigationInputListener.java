package ui.event;

import net.GopherItem.GopherItemType;

/*
    Listener for the events raised by the
    navigation bar ui element.
*/
public interface NavigationInputListener {
    void addressRequested(String addressText, GopherItemType contentType);
    void backwardRequested();
    void forwardRequested();
    void refreshRequested();
    void stopRequested();
}