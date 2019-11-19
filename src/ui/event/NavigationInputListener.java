package ui.event;

/*
    Listener for the events raised by the
    navigation bar ui element.
*/
public interface NavigationInputListener {
    void addressRequested();
    void backwardRequested();
    void forwardRequested();
    void refreshRequested();
    void stopRequested();
}