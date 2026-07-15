/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui.laf;

import juuxel.translationtool.gui.TriStateCheckBox;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalCheckBoxUI;
import java.awt.Component;
import java.awt.Graphics;

public final class TriStateCheckBoxUI extends MetalCheckBoxUI {
    private static final TriStateCheckBoxUI INSTANCE = new TriStateCheckBoxUI();

    public static void install() {
        UIManager.getDefaults().put(TriStateCheckBox.UI_CLASS_ID, TriStateCheckBoxUI.class.getName());
    }

    @SuppressWarnings("unused") // called by Swing
    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        icon = new IndeterminateCheckBoxIcon(UIManager.getIcon(getPropertyPrefix() + "icon"));
    }

    private static final class IndeterminateCheckBoxIcon implements Icon {
        private final Icon original;

        IndeterminateCheckBoxIcon(Icon original) {
            this.original = original;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            original.paintIcon(c, g, x, y);

            if (c instanceof TriStateCheckBox checkBox && checkBox.getState().isIndeterminate()) {
                g.fillRect(x + 3, y + 3, getIconWidth() - 6, getIconHeight() - 6);
            }
        }

        @Override
        public int getIconWidth() {
            return original.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return original.getIconHeight();
        }
    }
}
