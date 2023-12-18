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

package org.gophie.net.event;

import org.gophie.net.GopherPage;
import org.gophie.net.GopherUrl;
import org.gophie.net.GopherItem.GopherItemType;

public interface GopherClientEventListener {
    void progress(GopherUrl url, long byteCount);
    void pageLoaded(GopherPage result);
    void pageLoadFailed(GopherError error, GopherUrl url);
    void pageLoadItemMismatch(GopherItemType requested, GopherItemType detected, GopherUrl url);
}