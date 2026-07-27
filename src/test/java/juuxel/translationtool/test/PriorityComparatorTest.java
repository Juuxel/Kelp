/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package juuxel.translationtool.test;

import juuxel.translationtool.util.PriorityComparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityComparatorTest {
    private static final String PRIORITY_ELEMENT = "Priority";
    private static final String OTHER_1 = "Other 1";
    private static final String OTHER_2 = "Other 2";
    private static final PriorityComparator<String> COMPARATOR = new PriorityComparator<>(PRIORITY_ELEMENT);

    @Test
    void twoPriorityElementsEqual() {
        // noinspection EqualsWithItself
        assertEquals(0, COMPARATOR.compare(PRIORITY_ELEMENT, PRIORITY_ELEMENT));
    }

    @Test
    void priorityLessThanOther() {
        assertEquals(-1, COMPARATOR.compare(PRIORITY_ELEMENT, OTHER_1));
        assertEquals(1, COMPARATOR.compare(OTHER_1, PRIORITY_ELEMENT));
        assertEquals(-1, COMPARATOR.compare(PRIORITY_ELEMENT, OTHER_2));
        assertEquals(1, COMPARATOR.compare(OTHER_2, PRIORITY_ELEMENT));
    }

    @Test
    void othersEqual() {
        // noinspection EqualsWithItself
        assertEquals(0, COMPARATOR.compare(OTHER_1, OTHER_1));
        assertEquals(0, COMPARATOR.compare(OTHER_1, OTHER_2));
        assertEquals(0, COMPARATOR.compare(OTHER_2, OTHER_1));
    }
}
