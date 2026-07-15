/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.util;

public enum TriState {
    FALSE,
    TRUE,
    INDETERMINATE;

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isIndeterminate() {
        return this == INDETERMINATE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }
}
