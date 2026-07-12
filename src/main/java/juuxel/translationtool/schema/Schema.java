/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.schema;

import java.util.List;

public record Schema(List<Entry> entries) {
    public static final Gap GAP = new Gap();

    public sealed interface Entry {
    }

    public record Key(String translationKey) implements Entry {
    }

    public static final class Gap implements Entry {
        private Gap() {
        }
    }
}
