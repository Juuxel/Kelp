/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.util;

import java.util.Comparator;
import java.util.Objects;

public final class PriorityComparator<T> implements Comparator<T> {
    private final T priority;

    public PriorityComparator(T priority) {
        this.priority = priority;
    }

    @Override
    public int compare(T a, T b) {
        if (Objects.equals(a, priority)) {
            return Objects.equals(b, priority) ? 0 : -1;
        } else if (Objects.equals(b, priority)) {
            return 1;
        }

        return 0;
    }
}
