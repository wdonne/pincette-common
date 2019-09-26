package net.pincette.util;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This cache keeps objects for a limited amount of time. It is meant for a small number of short
 * lived objects.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 * @author Werner Donn\u00e9
 * @since 1.6
 */
public class TimedCache<K, V> {
  private final Map<K, Slot<V>> cache = new ConcurrentHashMap<>();
  private final Duration margin;
  private final Duration ttl;
  private Instant lastCleanUp;

  /**
   * Creates a cache with a twenty percent margin.
   *
   * @param ttl the time to live.
   */
  public TimedCache(final Duration ttl) {
    this(ttl, ofMillis(ttl.toMillis() / 5));
  }

  /**
   * Create a cache.
   *
   * @param ttl the time to live.
   * @param margin the amount of time an object is allowed to live longer than the <code>ttl</code>.
   *     This reduces the number of clean-ups.
   */
  public TimedCache(final Duration ttl, final Duration margin) {
    this.ttl = ttl;
    this.margin = margin;
  }

  private void cleanUp() {
    final Instant now = now();

    if (lastCleanUp == null || lastCleanUp.isBefore(now.minus(margin))) {
      cache.entrySet().stream()
          .filter(e -> e.getValue().timestamp.isBefore(now.minus(ttl)))
          .map(Map.Entry::getKey)
          .forEach(cache::remove);

      lastCleanUp = now;
    }
  }

  public Optional<V> get(final K key) {
    cleanUp();

    return Optional.ofNullable(cache.get(key)).map(s -> s.value);
  }

  public TimedCache<K, V> put(final K key, final V value) {
    cleanUp();
    cache.put(key, new Slot<>(value, now()));

    return this;
  }

  private static class Slot<T> {
    private final Instant timestamp;
    private final T value;

    private Slot(final T value, final Instant timestamp) {
      this.value = value;
      this.timestamp = timestamp;
    }
  }
}
