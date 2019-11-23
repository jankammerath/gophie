package org.gophie.net.event;

import org.gophie.net.GopherPage;
import org.gophie.net.GopherUrl;

public interface GopherClientEventListener {
    void progress(GopherUrl url, long byteCount);
    void pageLoaded(GopherPage result);
    void pageLoadFailed(GopherError error, GopherUrl url);
}