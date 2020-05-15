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

/**
 * This class allows to identify a file type by the file signature head in the
 * first few bytes of the file.
 */
public class FileSignature {
    /* enum with the available file types */
    public enum FileSignatureType {
        UNKNOWN, BINARY, IMAGE, MEDIA, TEXT
    }

    /* hex values for converting byte array to hex representation */
    private static final char[] HEX_VALUE = "0123456789ABCDEF".toCharArray();

    /* file signatures for popular image files */
    private static final String[] IMAGE_SIGNATURE_LIST = new String[]{
        /* GIF */               "47494638",
        /* JPEG */              "FFD8FF", 
        /* JPEG 2000 */         "0000000C6A502020",
        /* PNG */               "89504E470D0A1A0A",
        /* BMP */               "424D",
        /* ICO */               "00000100",
        /* TIFF */              "492049", "49492A00", "4D4D002A", "4D4D002B"
    };

    /* file signatures for popular media files */
    private static final String[] MEDIA_SIGNATURE_LIST = new String[]{
        /* AIFF */              "464F524D",
        /* OGG */               "4F676753",
        /* MP3 */               "494433", "FFFA", "FFFB", "FFF3", "FFF2",
        /* WAV, AVI */          "52494646",
        /* MIDI */              "4D546864",
        /* FLAC */              "664C6143",
        /* MKV */               "1A45DFA3",
        /* MPEG */              "000001BA", "000001B3",
        /* WMV, WMA, ASF */     "3026B2758E66CF11", "A6D900AA0062CE6C"
    };

    /* file signatures for popular binary files like zip, rar, tar, hqx */
    private static final String[] BINARY_SIGNATURE_LIST = new String[]{
        /* RAR */               "526172211A07",
        /* ZIP family */        "504B0304", "504B0506", "504B0708", "504B4C495445",
                                "504B537058", "504B0506", "504B0708", "57696E5A6970", 
                                "504B030414000100", "377ABCAF271C", "FD377A585A00",
                                "04224D18", "4D534346", "535A444488F02733",
        /* HQX */               "28546869",
        /* TAR */               "7573746172003030", "7573746172202000",
        /* GZIP */              "1F8B",
        /* DMG */               "7801730D626260",
        /* SIT */               "5349542100", "5374756666497420"
    };

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
     * Verifies if the provided hex string matches
     * any of the signatures from the list provided
     * 
     * @param list
     * String array with file signature list
     * 
     * @param hex
     * the hex string of the byte content
     * 
     * @return
     * true when any signature matches, otherwise false
     */
    private boolean hasSignature(String[] list, String hex){
        boolean result = false;

        /* check if the content contains an image signature */
        for(int i=0; i<list.length; i++){
            /* get the current signature and check if the content begins with it */
            String signature = list[i];
            if(hex.substring(0, signature.length()).equals(signature)){
                /* this file is an image */
                result = true;
            }
        }

        return result;
    }

    /**
     * Returns the signature type for this file signature
     * 
     * @return
     * FileSignatureType-enum defining the type
     */
    public FileSignatureType getSignatureItemType(){
        /* unknown file type by default */
        FileSignatureType result = FileSignatureType.UNKNOWN;

        /* get the hex representation of this file */
        String fileHex = this.getContentHexString();

        /* set the content type to text if it is text */
        if(this.isTextContent()){ result = FileSignatureType.TEXT; }

        /* check if the content contains an image signature */
        if(this.hasSignature(FileSignature.IMAGE_SIGNATURE_LIST, fileHex)){
            result = FileSignatureType.IMAGE;
        }

        /* check if the content contains a media file signature */
        if(this.hasSignature(FileSignature.MEDIA_SIGNATURE_LIST, fileHex)){
            result = FileSignatureType.MEDIA;
        }

        /* check if the content contains a binary file signature */
        if(this.hasSignature(FileSignature.BINARY_SIGNATURE_LIST, fileHex)){
            result = FileSignatureType.BINARY;
        }

        /* check if the file is an mpeg4 container */
        if(fileHex.substring(8, 16).equals("66747970")){ result = FileSignatureType.MEDIA; }

        return result;
    }

    /**
     * Determines whether the provided content is 
     * human readable text such as a text file or 
     * a gophermap and returns true when it is
     * 
     * @return
     */
    private boolean isTextContent(){
        boolean result = false;

        /* get the hex string for the byte content */
        String fileHex = this.getContentHexString();
        
        /* cound the alphanumeric chars in the data */
        double alphaNumCharCount = 0;
        double totalCharCount = 0;
        for(int c=0; c<fileHex.length(); c = c + 2){
            int charCode = Integer.parseInt(fileHex.substring(c, c+2),16);
            if(charCode != 0){
                if((charCode >= 65 && charCode <= 90) || (charCode >= 97 && charCode <= 122)){
                    alphaNumCharCount++;
                }
                totalCharCount++;
            }
        }

        /* when the percentage is greater than 20, it is highly likely
            that this is a text document. Images and binary files mainly 
            have 15% or less text coverage. */
        double percent = ((alphaNumCharCount / totalCharCount)*100);
        if(percent >= 20){ result = true; }

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