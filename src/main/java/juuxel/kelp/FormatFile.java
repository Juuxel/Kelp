/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.kelp;

import com.google.common.collect.MapMaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record FormatFile(ContentKinds contentKinds, List<TranslationGroup> groups) {
    private static final Map<FormatFile, TranslationKeyComparator> comparators = new MapMaker().weakKeys().makeMap();

    public FormatFile prependContentKinds(Collection<ContentKinds> kinds) {
        List<ContentKinds> cks = new ArrayList<>(kinds);
        cks.add(contentKinds);
        return new FormatFile(ContentKinds.merge(cks), groups);
    }

    public TranslationKeyComparator getTranslationKeyComparator() {
        return comparators.computeIfAbsent(this, TranslationKeyComparator::new);
    }
}
