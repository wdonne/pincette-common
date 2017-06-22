package net.pincette.util;

import java.util.Objects;
import java.util.function.Function;

import static net.pincette.util.Util.hash;



/**
 * An immutable pair of elements.
 * @author Werner Donn\u00e9
 */

public final class Pair<T,U>

{

  final public T first;
  final public U second;



  public
  Pair(final T first, final U second)
  {
    this.first = first;
    this.second = second;
  }



  @SuppressWarnings("unchecked")
  public boolean
  equals(final Object o)
  {
    return
      getClass().isAssignableFrom(o.getClass()) &&
        Objects.equals(first, ((Pair<T,U>) o).first) &&
        Objects.equals(second, ((Pair<T,U>) o).second);
  }



  public int
  hashCode()
  {
    return hash(first, second);
  }



  public static <T,U> Pair<T,U>
  pair(final T first, final U second)
  {
    return new Pair<>(first, second);
  }



  /**
   * Creates a pair with the first two elements. If the array has less than
   * two elements the corresponding elements in the pair will be
   * <code>null</code>.
   * @param array the given array.
   * @param map the function that is applied to the array value and the
   *            result of which is put in the pair.
   * @param <T> the array element type.
   * @param <R> the pair element type.
   * @return The new pair.
   */

  public static <T,R> Pair<R,R>
  toPair(final T[] array, final Function<T,R> map)
  {
    return
      new Pair<>
      (
        array.length > 0 ? map.apply(array[0]) : null,
        array.length > 1 ? map.apply(array[1]) : null
      );
  }

} // Pair
