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

package org.gophie.ui.event;

import org.gophie.net.GopherItem;
import org.gophie.net.GopherPage;

public interface PageMenuEventListener {
    void setHomeGopherRequested(String url);
    void itemDownloadRequested(GopherItem item);
    void pageSaveRequested(GopherPage page);
    void selectAllTextRequested();
    void getAndGoRequested(GopherPage page, String selected, boolean shifted);
    void closeRequested();
    void exitRequested();
}
