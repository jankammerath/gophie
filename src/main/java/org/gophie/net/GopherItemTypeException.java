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

package org.gophie.net;

import org.gophie.net.GopherItem.GopherItemType;

/**
 * Exception that indicates a mismatch between the
 * requested gopher item type and the actual detected
 * gopher item type during the request.
 */
public class GopherItemTypeException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String url;
    private final GopherItemType requested;
    private final GopherItemType detected;

    /**
     * Constructs this exception
     *
     * @param requestedUrl  the requested gopher url for this item
     * @param requestedType the requested gopher type for this item
     * @param detectedType  the detected gopher type for this item
     */
    public GopherItemTypeException(String requestedUrl, GopherItemType requestedType, GopherItemType detectedType) {
        this.url = requestedUrl;
        this.requested = requestedType;
        this.detected = detectedType;
    }

    /**
     * Returns the requested url
     *
     * @return url as string
     */
    public String getRequestedUrl() {
        return this.url;
    }

    /**
     * Returns the requested gopher item type
     *
     * @return the goper item type requested
     */
    public GopherItemType getRequestedType() {
        return this.requested;
    }

    /**
     * Returns the detected gopher item type
     *
     * @return the gopher item type detected
     */
    public GopherItemType getDetectedType() {
        return this.detected;
    }
}