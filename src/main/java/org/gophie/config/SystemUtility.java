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

package org.gophie.config;

public class SystemUtility {
    public static String getFileSizeString(float byteSize){
        String result = byteSize + " Byte";

        if(byteSize > 1000){
            result = String.format("%.2f",(float)(byteSize / 1000)) + " KB";
        }if(byteSize > 1000000){
            result = String.format("%.2f",(float)(byteSize / 1000000)) + " MB";
        }if(byteSize > 1000000000){
            result = String.format("%.2f",(float)(byteSize / 1000000000)) + " GB";
        }

        return result;
    }
}