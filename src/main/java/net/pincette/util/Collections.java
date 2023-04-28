package net.pincette.util;

import static java.lang.Integer.min;
import static java.util.Collections.nCopies;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.pincette.util.Array.inArray;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.StreamUtil.takeWhile;
import static net.pincette.util.Util.countingIterator;
import static net.pincette.util.Util.segments;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.pincette.function.SideEffect;

/**
 * Collection utilities.
 *
 * @author Werner Donn\u00e9
 */
public class Collections {
  private Collections() {}

  /**
   * Works like <code>Map.computeIfAbsent</code>, but avoids a concurrent access exception when the
   * given function also modifies the map.
   *
   * @param map the given map.
   * @param key the key to look up.
   * @param fn the function that generates a new value if it doesn't exist for the given key.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The existing or generated value.
   * @since 1.8.1
   */
  public static <K, V> V computeIfAbsent(
      final Map<K, V> map, final K key, final Function<? super K, ? extends V> fn) {
    return ofNullable(map.get(key))
        .orElseGet(
            () ->
                Optional.of(fn.apply(key))
                    .map(v -> SideEffect.<V>run(() -> map.put(key, v)).andThenGet(() -> v))
                    .orElse(null));
  }

  /**
   * Works like <code>Map.computeIfPresent</code>, but avoids a concurrent access exception when the
   * given function also modifies the map.
   *
   * @param map the given map.
   * @param key the key to look up.
   * @param fn the function that generates a new value if one already exists for the given key.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new value or <code>null</code>.
   * @since 1.8.1
   */
  public static <K, V> V computeIfPresent(
      final Map<K, V> map, final K key, final BiFunction<? super K, ? super V, ? extends V> fn) {
    return ofNullable(map.get(key))
        .map(
            v ->
                ofNullable(fn.apply(key, v))
                    .map(val -> SideEffect.<V>run(() -> map.put(key, v)).andThenGet(() -> val))
                    .orElseGet(
                        () -> SideEffect.<V>run(() -> map.remove(key)).andThenGet(() -> null)))
        .orElse(null);
  }

  /**
   * Concatenates <code>collections</code> into one list containing the elements of the collections.
   *
   * @param collections the given collections.
   * @param <T> the element type.
   * @return The new list.
   * @since 1.7
   */
  @SafeVarargs
  public static <T> List<T> concat(final Collection<T>... collections) {
    return concat(Arrays.stream(collections));
  }

  /**
   * Concatenates <code>collections</code> into one list containing the elements of the collections.
   *
   * @param collections the given collections.
   * @param <T> the element type.
   * @return The new list.
   * @since 1.7
   */
  public static <T> List<T> concat(final Stream<Collection<T>> collections) {
    return collections.flatMap(Collection::stream).collect(toList());
  }

  private static <T> Stream<T> consumeDeque(final Deque<T> deque, final Function<Deque<T>, T> fn) {
    return takeWhile(fn.apply(deque), v -> fn.apply(deque), Objects::nonNull);
  }

  /**
   * Returns a stream that consumes all the elements starting from the head of the queue. The
   * returned elements are removed.
   *
   * @param deque the given queue.
   * @param <T> the value type.
   * @return The value stream.
   * @since 2.0.2
   */
  public static <T> Stream<T> consumeHead(final Deque<T> deque) {
    return consumeDeque(deque, Deque::pollFirst);
  }

  /**
   * Returns a stream that consumes all the elements starting from the head of the queue. The
   * returned elements are removed.
   *
   * @param deque the given queue.
   * @param <T> the value type.
   * @return The value stream.
   * @since 2.0.2
   */
  public static <T> Stream<T> consumeTail(final Deque<T> deque) {
    return consumeDeque(deque, Deque::pollLast);
  }

  /**
   * Returns a new set with the elements of <code>c1</code> but without those that also occur in
   * <code>c2</code>.
   *
   * @param c1 the first collection.
   * @param c2 the second collection.
   * @param <T> the type of the elements in the collections and the result.
   * @return The new set.
   */
  public static <T> Set<T> difference(final Collection<T> c1, final Collection<T> c2) {
    final Set<T> result = new HashSet<>(c1);

    result.removeAll(c2);

    return result;
  }

  /**
   * Returns a map where all keys that are paths with segments separated by the literal string
   * <code>delimiter</code> are replaced with the first segment as the key and an expanded submap
   * with the remainder of the segments as the value.
   *
   * @param map the given map.
   * @param delimiter the literal string that separates the keys in the given map.
   * @return The new expanded map.
   * @since 1.8
   */
  public static Map<String, Object> expand(final Map<String, ?> map, final String delimiter) {
    return expand(map, s -> segments(s, delimiter), CharSequence::toString);
  }

  /**
   * Returns a map where all keys that are paths with segments separated by <code>delimiter</code>
   * are replaced with the first segment as the key and an expanded submap with the remainder of the
   * segments as the value.
   *
   * @param map the given map.
   * @param delimiter the delimiter for the keys in the given map.
   * @return The new expanded map.
   * @since 2.0.4
   */
  public static Map<CharSequence, Object> expand(
      final Map<String, ?> map, final Pattern delimiter) {
    return expand(map, s -> segments(s, delimiter), k -> k);
  }

  public static <T> Map<T, Object> expand(
      final Map<String, ?> map, final Pattern delimiter, final Function<CharSequence, T> key) {
    return expand(map, s -> segments(s, delimiter), key);
  }

  private static <T> Map<T, Object> expand(
      final Map<String, ?> map,
      final Function<String, Stream<CharSequence>> segments,
      final Function<CharSequence, T> key) {
    final Map<T, Object> result = new HashMap<>();

    map.forEach(
        (k, v) -> {
          final List<CharSequence> segs = segments.apply(k).collect(toList());

          segs.subList(0, segs.size() - 1).stream()
              .reduce(
                  result,
                  (m, segment) ->
                      (Map<T, Object>) m.computeIfAbsent(key.apply(segment), s -> new HashMap<>()),
                  (m1, m2) -> m1)
              .put(key.apply(segs.get(segs.size() - 1)), v);
        });

    return result;
  }

  /**
   * Creates a new map with only the retained entries.
   *
   * @param map the given map.
   * @param retain the predicate that decides which entries remain.
   * @return The new map.
   * @param <K> the key type.
   * @param <V> the value type.
   * @since 2.3.2
   */
  public static <K, V> Map<K, V> filterMap(
      final Map<K, V> map, final Predicate<Entry<K, V>> retain) {
    return map.entrySet().stream().filter(retain).collect(toMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Returns a map where the keys are paths created by the keys of the maps and the submaps,
   * separated by <code>delimiter</code>. The keys of all the submaps must be strings too. The
   * result is a map without submaps.
   *
   * @param map the given map.
   * @param delimiter the delimiter for the keys in the new map.
   * @return The new flattened map.
   * @since 1.8
   */
  public static Map<String, Object> flatten(final Map<String, ?> map, final String delimiter) {
    return map.entrySet().stream()
        .flatMap(
            e ->
                e.getValue() instanceof Map
                    ? flatten((Map<String, ?>) e.getValue(), delimiter).entrySet().stream()
                        .map(sub -> pair(e.getKey() + delimiter + sub.getKey(), sub.getValue()))
                    : Stream.of(pair(e.getKey(), e.getValue())))
        .collect(toMap(p -> p.first, p -> p.second));
  }

  /**
   * Returns an optional value for a key. The value will be empty is the key doesn't exist.
   *
   * @param map the map that is queried.
   * @param key the query key.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The optional value.
   */
  public static <K, V> Optional<V> get(final Map<K, V> map, final K key) {
    return ofNullable(map.get(key));
  }

  /**
   * Returns a stream of pairs for a list, where the second element of the pair is the zero-based
   * position of the element in the list.
   *
   * @param list the list of which the stream is made.
   * @param <T> the type of the elements in the list.
   * @return The stream of pairs.
   */
  public static <T> Stream<Pair<T, Integer>> indexedStream(final List<T> list) {
    return stream(countingIterator(list.iterator()));
  }

  /**
   * Returns a new set containing all elements that are common in the given collections.
   *
   * @param collections the given collections.
   * @param <T> the element type.
   * @return The intersection.
   */
  @SafeVarargs
  public static <T> Set<T> intersection(final Collection<T>... collections) {
    return intersection(Arrays.stream(collections));
  }

  /**
   * Returns a new set containing all elements that are common in the given collections.
   *
   * @param collections the given collections.
   * @param <T> the element type.
   * @return The intersection.
   * @since 1.7
   */
  public static <T> Set<T> intersection(final Stream<Collection<T>> collections) {
    return collections
        .map(HashSet::new)
        .reduce((s1, s2) -> SideEffect.<HashSet<T>>run(() -> s1.retainAll(s2)).andThenGet(() -> s1))
        .map(s -> (Set<T>) s)
        .orElseGet(java.util.Collections::emptySet);
  }

  /**
   * Creates an immutable list with the given elements.
   *
   * @param elements the given elements.
   * @param <T> the element type.
   * @return The new list.
   */
  @SafeVarargs
  public static <T> List<T> list(final T... elements) {
    return List.of(elements);
  }

  /**
   * Creates a map with the given element pairs, where each first element is a key and each second
   * element a value.
   *
   * @param pairs the given pairs.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new map.
   */
  @SafeVarargs
  public static <K, V> Map<K, V> map(final Pair<K, V>... pairs) {
    return map(Arrays.stream(pairs));
  }

  /**
   * Creates a map with the given element pairs, where each first element is a key and each second
   * element a value.
   *
   * @param pairs the given pairs.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new map.
   * @since 1.7
   */
  public static <K, V> Map<K, V> map(final Stream<Pair<K, V>> pairs) {
    return pairs.collect(toMap(pair -> pair.first, pair -> pair.second));
  }

  /**
   * Returns a new map with all the mappings of the given maps combined. When there is more than one
   * mapping for a key only the last one will be retained. The merge is recursive.
   *
   * @param maps the given maps.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new map.
   */
  @SafeVarargs
  public static <K, V> Map<K, V> merge(final Map<K, V>... maps) {
    return merge(Arrays.stream(maps));
  }

  /**
   * Returns a new map with all the mappings of the given maps combined. When there is more than one
   * mapping for a key only the last one will be retained. The merge is recursive.
   *
   * @param maps the given maps.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new map.
   * @since 1.7
   */
  public static <K, V> Map<K, V> merge(final Stream<Map<K, V>> maps) {
    return maps.flatMap(m -> m.entrySet().stream())
        .collect(
            toMap(
                Entry::getKey,
                Entry::getValue,
                (v1, v2) ->
                    v1 instanceof Map && v2 instanceof Map
                        ? (V) merge(Stream.of((Map) v1, (Map) v2))
                        : v2));
  }

  /**
   * Returns a set of pairs where each element in <code>s1</code> is combined with each element in
   * <code>s2</code>.
   *
   * @param s1 the elements for the first elements of the pairs.
   * @param s2 the elements for the second elements of the pairs.
   * @param <T> the element type of the first set.
   * @param <U> the element type of the second set.
   * @return The new set.
   */
  public static <T, U> Set<Pair<T, U>> multiply(final Set<T> s1, final Set<U> s2) {
    return s1.stream().flatMap(el1 -> s2.stream().map(el2 -> pair(el1, el2))).collect(toSet());
  }

  /**
   * Returns a new map with the added mapping.
   *
   * @param map the original map.
   * @param key the new key.
   * @param value the new value.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new map.
   */
  public static <K, V> Map<K, V> put(final Map<K, V> map, final K key, final V value) {
    final Map<K, V> result = new HashMap<>(map);

    result.put(key, value);

    return result;
  }

  /**
   * Returns a new map, which hasn't mappings for the given keys.
   *
   * @param map the original map.
   * @param keys the keys that are to be removed.
   * @param <K> the key type.
   * @param <V> the value type.
   * @return The new map.
   */
  @SafeVarargs
  public static <K, V> Map<K, V> remove(final Map<K, V> map, final K... keys) {
    return filterMap(map, e -> !inArray(keys, e.getKey()));
  }

  /**
   * Returns an iterator that iterates over the list in reverse order.
   *
   * @param list the given list.
   * @param <T> the element type.
   * @return The iterator.
   */
  public static <T> Iterator<T> reverse(final List<T> list) {
    return list.isEmpty() ? list.iterator() : new ReverseIterator<>(list);
  }

  /**
   * Creates a set with the given elements.
   *
   * @param elements the given elements.
   * @param <T> the element type.
   * @return The new set.
   */
  @SafeVarargs
  public static <T> Set<T> set(final T... elements) {
    return Arrays.stream(elements).collect(toSet());
  }

  /**
   * Removes the first <code>positions</code> elements from the list and adds the last element
   * <code>positions</code> times at the end of the list.
   *
   * @param list the given list.
   * @param positions the number of positions over which to shift. If it is larger than the list
   *     size it will be reduced to the list size.
   * @param <T> the element type.
   * @return The new list.
   * @since 1.7
   */
  public static <T> List<T> shiftDown(final List<T> list, final int positions) {
    return !list.isEmpty() ? shiftDown(list, positions, list.get(list.size() - 1)) : list;
  }

  /**
   * Removes the first <code>positions</code> elements from the list and adds <code>newElement
   * </code> <code>positions</code> times at the end of the list.
   *
   * @param list the given list.
   * @param positions the number of positions over which to shift. If it is larger than the list
   *     size it will be reduced to the list size.
   * @param newElement the element that is shifted in.
   * @param <T> the element type.
   * @return The new list.
   * @since 1.7
   */
  public static <T> List<T> shiftDown(final List<T> list, final int positions, final T newElement) {
    final int pos = min(positions, list.size());

    return !list.isEmpty()
        ? concat(list.subList(pos, list.size()), nCopies(pos, newElement))
        : list;
  }

  /**
   * Removes the last <code>positions</code> elements from the list and adds the first element
   * <code>positions</code> times at the start of the list.
   *
   * @param list the given list.
   * @param positions the number of positions over which to shift. If it is larger than the list
   *     size it will be reduced to the list size.
   * @param <T> the element type.
   * @return The new list.
   * @since 1.7
   */
  public static <T> List<T> shiftUp(final List<T> list, final int positions) {
    return !list.isEmpty() ? shiftUp(list, positions, list.get(0)) : list;
  }

  /**
   * Removes the last <code>positions</code> elements from the list and adds <code>newElement
   * </code> <code>positions</code> times at the start of the list.
   *
   * @param list the given list.
   * @param positions the number of positions over which to shift. If it is larger than the list
   *     size it will be reduced to the list size.
   * @param newElement the element that is shifted in.
   * @param <T> the element type.
   * @return The new list.
   * @since 1.7
   */
  public static <T> List<T> shiftUp(final List<T> list, final int positions, final T newElement) {
    final int pos = min(positions, list.size());

    return !list.isEmpty()
        ? concat(nCopies(pos, newElement), list.subList(0, list.size() - pos))
        : list;
  }

  /**
   * Returns a new set containing all of the elements from the given collections.
   *
   * @param collections the given collections.
   * @param <T> the element type.
   * @return The new set.
   */
  @SafeVarargs
  public static <T> Set<T> union(final Collection<T>... collections) {
    return union(Arrays.stream(collections));
  }

  /**
   * Returns a new set containing all of the elements from the given collections.
   *
   * @param collections the given collections.
   * @param <T> the element type.
   * @return The new set.
   * @since 1.7
   */
  public static <T> Set<T> union(final Stream<Collection<T>> collections) {
    return collections.flatMap(Collection::stream).collect(toSet());
  }

  private static class ReverseIterator<T> implements Iterator<T> {
    private final ListIterator<T> iterator;

    private ReverseIterator(final List<T> list) {
      iterator = list.listIterator(list.size());
    }

    public boolean hasNext() {
      return iterator.hasPrevious();
    }

    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return iterator.previous();
    }
  }
}
