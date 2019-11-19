package net.event;

import net.GopherPage;

public interface GopherClientEventListener {
    void pageLoaded(GopherPage result);
    void pageLoadFailed(GopherError error);
}