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

package org.gophie;

import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import org.gophie.ui.MainWindow;

import javax.swing.*;

public class Gophie {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        MainWindow window = new MainWindow();
        window.show();
    }

    public static void main(String[] args) {
        /* remove the borders for the pane */
        UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.getDefaults().put("ScrollPane.border", BorderFactory.createEmptyBorder());

        /* set to use the mac menu bar instead */
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        /* set the proper application title on mac */
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Gophie");

        //FlatNightOwlContrastIJTheme.setup();
        FlatArcDarkOrangeIJTheme.setup();

        /* Schedule a job for the event-dispatching thread:
            creating and showing this application's GUI. */
        javax.swing.SwingUtilities.invokeLater(Gophie::createAndShowGUI);
    }
}