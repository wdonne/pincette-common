package net.pincette.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.pincette.util.Util.countingIterator;
import static net.pincette.util.Util.stream;



/**
 * Collection utilities.
 * @author Werner Donn\u00e9
 */

public class Collections

{

  public static <T> Set<T>
  difference(final Collection<T> c1, final Collection<T> c2)
  {
    final Set<T> result = new HashSet<>(c1);

    result.removeAll(c2);

    return result;
  }



  public static <K,V> Optional<V>
  get(final Map<K,V> map, final K key)
  {
    return Optional.ofNullable(map.get(key));
  }



  public static <T> Stream<Pair<T,Integer>>
  indexedStream(final List<T> list)
  {
    return stream(countingIterator(list.iterator()));
  }



  @SafeVarargs
  public static <T> Set<T>
  intersection(final Collection<T>... collections)
  {
    final Set<T> result = new HashSet<>();

    Arrays.stream(collections).forEach(result::retainAll);

    return result;
  }



  @SafeVarargs
  public static <T> List<T>
  list(final T... elements)
  {
    return Arrays.stream(elements).collect(toList());
  }



  @SafeVarargs
  public static <K,V> Map<K,V>
  map(final Pair<K,V>... pairs)
  {
    return
      Arrays.
        stream(pairs).collect(toMap(pair -> pair.first, pair -> pair.second));
  }



  @SafeVarargs
  public static <K,V> Map<K,V>
  merge(final Map<K,V>... maps)
  {
    final Map<K,V> result = new HashMap<>();

    Arrays.stream(maps).forEach(result::putAll);

    return result;
  }



  public static <T,U> Set<Pair<T,U>>
  multiply(final Set<T> s1, final Set<U> s2)
  {
    return
      s1.stream().
        flatMap(el1 -> s2.stream().map(el2 -> new Pair<>(el1, el2))).
        collect(toSet());
  }



  /**
   * Returns a new map.
   */

  public static <K,V> Map<K,V>
  put(final Map<K,V> map, final K key, final V value)
  {
    final Map<K,V> result = new HashMap<>(map);

    result.put(key, value);

    return result;
  }



  /**
   * Returns a new map.
   */

  @SafeVarargs
  public static <K,V> Map<K,V>
  remove(final Map<K,V> map, final K... keys)
  {
    final Map<K,V> result = new HashMap<>(map);

    Arrays.stream(keys).forEach(result::remove);

    return result;
  }



  public static <T> Iterator<T>
  reverse(final List<T> list)
  {
    return list.size() == 0 ? list.iterator() : new ReverseIterator(list);
  }



  @SafeVarargs
  public static <T> Set<T>
  set(final T... objects)
  {
    return Arrays.stream(objects).collect(toSet());
  }



  @SafeVarargs
  public static <T> Set<T>
  union(final Collection<T>... collections)
  {
    final Set<T> result = new HashSet<>();

    Arrays.stream(collections).forEach(result::addAll);

    return result;
  }



  private static class ReverseIterator<T> implements Iterator<T>

  {

    private final ListIterator<T> iterator;



    private
    ReverseIterator(final List<T> list)
    {
      iterator = list.listIterator(list.size());
    }



    public boolean
    hasNext()
    {
      return iterator.hasPrevious();
    }



    public T
    next()
    {
      return iterator.previous();
    }

  } // ReverseIterator

} // Collections
