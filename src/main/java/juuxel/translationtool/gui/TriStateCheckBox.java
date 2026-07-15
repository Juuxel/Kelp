/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.gui;

import juuxel.translationtool.util.TriState;

import javax.swing.JCheckBox;

public class TriStateCheckBox extends JCheckBox {
    public static final String UI_CLASS_ID = "TriStateCheckBoxUI";

    public TriStateCheckBox() {
        setModel(new Model());
    }

    public TriState getState() {
        if (getModel() instanceof Model m) {
            return m.getState();
        }

        return isSelected() ? TriState.TRUE : TriState.FALSE;
    }

    public void setState(TriState state) {
        if (getModel() instanceof Model m) {
            m.setState(state);
        }
    }

    @Override
    public String getUIClassID() {
        return UI_CLASS_ID;
    }

    public static final class Model extends ToggleButtonModel {
        private TriState state = TriState.FALSE;

        @Override
        public void setSelected(boolean b) {
            setState(b ? TriState.TRUE : TriState.FALSE);
        }

        public TriState getState() {
            return state;
        }

        public void setState(TriState state) {
            this.state = state;
            super.setSelected(state.isTrue());
        }
    }
}
