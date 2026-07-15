/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import javax.swing.JFrame;

public interface App {
    void setUndoRedoEnabled(boolean undo, boolean redo);
    JFrame getFrame();
    void refreshTitle();
    void showErrorPopup(Exception e);
}
