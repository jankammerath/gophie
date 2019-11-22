package net.event;

import net.GopherPage;
import net.GopherUrl;

public interface GopherClientEventListener {
    void pageLoaded(GopherPage result);
    void pageLoadFailed(GopherError error, GopherUrl url);
}