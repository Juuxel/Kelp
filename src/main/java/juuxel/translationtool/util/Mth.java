/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.util;

public final class Mth {
    public static boolean isIncreasingInterval(int[] xs) {
        if (xs.length == 0) return false;

        int prev = xs[0];

        for (int i = 1; i < xs.length; i++) {
            if (xs[i] != prev + 1) return false;

            prev = xs[i];
        }

        return true;
    }
}
