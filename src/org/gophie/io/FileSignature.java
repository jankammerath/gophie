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

package org.gophie.io;
import org.gophie.net.GopherItem.GopherItemType;

/**
 * This class allows to identify a file type by the file signature head in the
 * first few bytes of the file.
 */
public class FileSignature {
    /* enum with the available file types */
    public enum FileSignatureType {
        /* image file signature types */
        IMAGE_JPEG, IMAGE_PNG, IMAGE_GIF, IMAGE_BMP, IMAGE_ICO, IMAGE_TIFF,

        /* audio and video file signature types */
        MEDIA_AIFF, MEDIA_OGG, MEDIA_MP3, MEDIA_WAV, MEDIA_AVI, MEDIA_FLAC, 
        MEDIA_MKV, MEDA_MPEG, MEDIA_MINI,

        /* gopher map and text file signature types */
        TEXT_GOPHERMAP, TEXT_PLAIN,

        /* defines any other file type */
        BINARY_UNKNOWN
    }

    /* hex values for converting byte array to hex representation */
    private static final char[] HEX_VALUE = "0123456789ABCDEF".toCharArray();

    /* content for this file signature */
    private byte[] content;

    /**
     * Constructs the signature object and sets the content locally
     * 
     * @param fileContent
     */
    public FileSignature(byte[] fileContent){
        this.content = fileContent;
    }

    /**
     * Returns the signature type for this file signature
     * 
     * @return
     * FileSignatureType-enum defining the type
     */
    public GopherItemType getSignatureItemType(){
        /* default binary file is the default return value */
        GopherItemType result = GopherItemType.BINARY_FILE;

        /* get the hex representation of this file */
        String fileHex = this.getContentHexString();
        System.out.println(fileHex);

        return result;
    }

    /**
     * Returns the content as a hex string representation
     * 
     * @return
     */
    private String getContentHexString() {
        char[] hexChars = new char[this.content.length * 2];
        for (int j = 0; j < this.content.length; j++) {
            int v = this.content[j] & 0xFF;
            hexChars[j * 2] = FileSignature.HEX_VALUE[v >>> 4];
            hexChars[j * 2 + 1] = FileSignature.HEX_VALUE[v & 0x0F];
        }
        return new String(hexChars);
    }
}