package org.gophie.ui.event;

import org.gophie.net.GopherItem;

/*
    Listener for the events raised by the
    navigation bar ui element.
*/
public interface NavigationInputListener {
    void addressRequested(String addressText, GopherItem item);
    void backwardRequested();
    void forwardRequested();
    void refreshRequested();
    void stopRequested();
    void showDownloadRequested();
}