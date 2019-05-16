package net.pincette.util;

import static java.util.Objects.hash;

import java.util.Objects;
import java.util.function.Function;

/**
 * An immutable triple of elements.
 *
 * @author Werner Donn\u00e9
 */
public final class Triple<T, U, V> {

  public final T first;
  public final U second;
  public final V third;

  public Triple(final T first, final U second, final V third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public static <T, U, V> Triple<T, U, V> triple(final T first, final U second, final V third) {
    return new Triple<>(first, second, third);
  }

  /**
   * Creates a triple with the first three elements. If the array has less than three elements the
   * corresponding elements in the triple will be <code>null</code>.
   *
   * @param array the given array.
   * @param map the function that is applied to the array value and the result of which is put in
   *     the triple.
   * @param <T> the array element type.
   * @param <R> the triple element type.
   * @return The new triple.
   */
  public static <T, R> Triple<R, R, R> toTriple(final T[] array, final Function<T, R> map) {
    return new Triple<>(
        array.length > 0 ? map.apply(array[0]) : null,
        array.length > 1 ? map.apply(array[1]) : null,
        array.length > 2 ? map.apply(array[2]) : null);
  }

  @SuppressWarnings("unchecked")
  public boolean equals(final Object o) {
    return o instanceof Triple
        && getClass().isAssignableFrom(o.getClass())
        && Objects.equals(first, ((Triple<T, U, V>) o).first)
        && Objects.equals(second, ((Triple<T, U, V>) o).second)
        && Objects.equals(third, ((Triple<T, U, V>) o).third);
  }

  public int hashCode() {
    return hash(first, second, third);
  }
}
