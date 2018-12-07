package net.pincette.util;

import static java.util.Objects.hash;

import java.util.Objects;
import java.util.function.Function;

/**
 * An immutable pair of elements.
 *
 * @author Werner Donn\u00e9
 */
public final class Pair<T, U> {
  public final T first;
  public final U second;

  public Pair(final T first, final U second) {
    this.first = first;
    this.second = second;
  }

  public static <T, U> Pair<T, U> pair(final T first, final U second) {
    return new Pair<>(first, second);
  }

  /**
   * Creates a pair with the first two elements. If the array has less than two elements the
   * corresponding elements in the pair will be <code>null</code>.
   *
   * @param array the given array.
   * @param map the function that is applied to the array value and the result of which is put in
   *     the pair.
   * @param <T> the array element type.
   * @param <R> the pair element type.
   * @return The new pair.
   */
  public static <T, R> Pair<R, R> toPair(final T[] array, final Function<T, R> map) {
    return new Pair<>(
        array.length > 0 ? map.apply(array[0]) : null,
        array.length > 1 ? map.apply(array[1]) : null);
  }

  @SuppressWarnings("unchecked")
  public boolean equals(final Object o) {
    return o != null
        && o instanceof Pair
        && getClass().isAssignableFrom(o.getClass())
        && Objects.equals(first, ((Pair<T, U>) o).first)
        && Objects.equals(second, ((Pair<T, U>) o).second);
  }

  public int hashCode() {
    return hash(first, second);
  }
}
