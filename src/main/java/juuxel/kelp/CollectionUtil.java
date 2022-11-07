/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.kelp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CollectionUtil {
    public static <E> int indexOf(List<E> es, Predicate<? super E> predicate) {
        for (int i = 0; i < es.size(); i++) {
            if (predicate.test(es.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public static <A, B, C extends Collection<? super B>> C mapTo(Iterable<A> as, Function<? super A, ? extends B> transform, C collection) {
        for (A a : as) {
            collection.add(transform.apply(a));
        }

        return collection;
    }

    public static <A, B> List<B> map(Iterable<A> as, Function<? super A, ? extends B> transform) {
        List<B> result = as instanceof Collection<A> c ? new ArrayList<>(c.size()) : new ArrayList<>();
        return mapTo(as, transform, result);
    }

    public static <D, K, V> Map<K, V> buildMap(Iterable<D> data, Function<? super D, ? extends K> keySelector, Function<? super D, ? extends V> valueSelector) {
        Map<K, V> map = new HashMap<>();

        for (D d : data) {
            map.put(keySelector.apply(d), valueSelector.apply(d));
        }

        return map;
    }
}
