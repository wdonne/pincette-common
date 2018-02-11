package net.pincette.util;

import static net.pincette.util.Pair.pair;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtil {

  private StreamUtil() {
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than
   * <code>toInclusive</code> then the stream will count down.
   *
   * @param from first value of the range.
   * @param toInclusive last value of the range.
   * @return the integer stream.
   */

  public static Stream<Integer>
  range(final int from, final int toInclusive) {
    return
        stream(
            new Iterator<Integer>() {
              private int index = from;

              @Override
              public boolean hasNext() {
                return from < toInclusive ? index <= toInclusive : index >= toInclusive;
              }

              @Override
              public Integer next() {
                if (!hasNext()) {
                  throw new NoSuchElementException();
                }

                return from < toInclusive ? index++ : index--;
              }
            }
        );
  }

  public static <T> Stream<T>
  stream(final Iterator<T> iterator) {
    return
        StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE
            ),
            false
        );
  }

  public static <T> Stream<T>
  stream(final Enumeration<T> enumeration) {
    return
        stream(
            new Iterator<T>() {
              private boolean more;

              @Override
              public boolean
              hasNext() {
                more = enumeration.hasMoreElements();

                return more;
              }

              @Override
              public T next() {
                if (!more) {
                  throw new NoSuchElementException();
                }

                return enumeration.nextElement();
              }
            }
        );
  }

  /**
   * Iterates sequentially until the predicate returns <code>false</code>.
   *
   * @param seed the initial value.
   * @param f the function that calculates the next value.
   * @param p the predicate.
   * @param <T> the value type.
   * @return The generated stream.
   */

  public static <T> Stream<T>
  takeWhile(final T seed, final UnaryOperator<T> f, final Predicate<T> p) {
    return
        stream(
            new Iterator<T>() {
              private T current = seed;
              private boolean ok;

              @Override
              public boolean hasNext() {
                ok = p.test(current);

                return ok;
              }

              @Override
              public T next() {
                if (!ok) {
                  throw new NoSuchElementException();
                }

                final T result = current;

                current = f.apply(current);

                return result;
              }
            }
        );
  }

  /**
   * Returns a stream that returns pairs of values of the respective streams. The stream ends as
   * soon as one the given streams ends.
   *
   * @param s1 the first stream.
   * @param s2 the second stream.
   * @param <T> the element type of the first stream.
   * @param <U> the element type of the second stream.
   * @return The paired stream.
   */

  public static <T, U> Stream<Pair<T, U>>
  zip(final Stream<T> s1, final Stream<U> s2) {
    return
        stream(
            new Iterator<Pair<T, U>>() {
              final Iterator<T> i1 = s1.iterator();
              final Iterator<U> i2 = s2.iterator();
              private boolean more;

              @Override
              public boolean hasNext() {
                more = i1.hasNext() && i2.hasNext();

                return more;
              }

              @Override
              public Pair<T, U> next() {
                if (!more) {
                  throw new NoSuchElementException();
                }

                return pair(i1.next(), i2.next());
              }
            }
        );
  }

} // StreamUtil
