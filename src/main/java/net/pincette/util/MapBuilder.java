package net.pincette.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MapBuilder<K, V> {
  private final Map<K, V> map;

  /**
   * Builds a copy of <code>map</code>.
   *
   * @param map the copied map.
   */
  public MapBuilder(final Map<? extends K, ? extends V> map) {
    this.map = new HashMap<>(map);
  }

  public Map<K, V> build() {
    return map;
  }

  public MapBuilder<K, V> merge(
      final K key,
      final V value,
      final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    map.merge(key, value, remappingFunction);

    return this;
  }

  public MapBuilder<K, V> put(final K key, final V value) {
    map.put(key, value);

    return this;
  }

  public MapBuilder<K, V> putAll(final Map<? extends K, ? extends V> m) {
    map.putAll(m);

    return this;
  }

  public MapBuilder<K, V> putIfAbsent(final K key, final V value) {
    map.putIfAbsent(key, value);

    return this;
  }

  public MapBuilder<K, V> remove(final Object key) {
    map.remove(key);

    return this;
  }

  public MapBuilder<K, V> remove(final Object key, final Object value) {
    map.remove(key, value);

    return this;
  }

  public MapBuilder<K, V> replace(final K key, final V value) {
    map.replace(key, value);

    return this;
  }

  public MapBuilder<K, V> replace(final K key, final V oldValue, final V newValue) {
    map.replace(key, oldValue, newValue);

    return this;
  }

  public MapBuilder<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    map.replaceAll(function);

    return this;
  }
}
