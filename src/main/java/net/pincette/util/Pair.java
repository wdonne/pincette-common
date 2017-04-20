package net.pincette.util;

import java.util.Objects;
import java.util.function.Function;



/**
 * @author Werner Donn\u00e9
 */

public final class Pair<T,U>

{

  final public T first;
  final public U second;



  public
  Pair(T first, U second)
  {
    this.first = first;
    this.second = second;
  }



  @SuppressWarnings("unchecked")
  public boolean
  equals(Object o)
  {
    return
      getClass().isAssignableFrom(o.getClass()) &&
        Objects.equals(first, ((Pair<T,U>) o).first) &&
        Objects.equals(second, ((Pair<T,U>) o).second);
  }



  public int
  hashCode()
  {
    return
      41 *
        (
          41 * (41 + (first != null ? first.hashCode() : 0)) +
            (second != null ? second.hashCode() : 0)
        );
  }



  /**
   * Creates a pair with the first two elements.
   */

  public static <T,R> Pair<R,R>
  toPair(final T[] array, final Function<T,R> map)
  {
    return
      new Pair<R,R>
      (
        array.length > 0 ? map.apply(array[0]) : null,
        array.length > 1 ? map.apply(array[1]) : null
      );
  }

} // Pair
