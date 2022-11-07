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
