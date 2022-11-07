/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.kelp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

final class Cache<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final Function<K, V> fetcher;

    Cache(Function<K, V> fetcher) {
        this.fetcher = fetcher;
    }

    V get(K key) {
        return map.computeIfAbsent(key, fetcher);
    }
}
