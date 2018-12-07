package net.pincette.util;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static net.pincette.util.Pair.pair;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import java.util.stream.StreamSupport;

/**
 * Some stream API untilities.
 *
 * @author Werner Donn\u00e9
 */
public class StreamUtil {
  private StreamUtil() {}

  /**
   * Returns the last element of a stream.
   *
   * @param stream the given stream.
   * @param <T> the element type.
   * @return the last element.
   */
  public static <T> Optional<T> last(final Stream<T> stream) {
    return Optional.ofNullable(stream.sequential().reduce(null, (result, element) -> element));
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is excluded.
   * @return the integer stream.
   */
  public static Stream<Long> rangeExclusive(final long from, final long to) {
    return stream(new RangeIterator(from, to, false)).map(Number::longValue);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is excluded.
   * @return the integer stream.
   */
  public static Stream<Integer> rangeExclusive(final int from, final int to) {
    return stream(new RangeIterator(from, to, false)).map(Number::intValue);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is included.
   * @return the integer stream.
   */
  public static Stream<Long> rangeInclusive(final long from, final long to) {
    return stream(new RangeIterator(from, to, true)).map(Number::longValue);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is included.
   * @return the integer stream.
   */
  public static Stream<Integer> rangeInclusive(final int from, final int to) {
    return stream(new RangeIterator(from, to, true)).map(Number::intValue);
  }

  public static <T> Stream<T> stream(final Iterator<T> iterator) {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
            iterator, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE),
        false);
  }

  public static <T> Stream<T> stream(final Enumeration<T> enumeration) {
    return stream(
        new Iterator<T>() {
          private boolean more;

          @Override
          public boolean hasNext() {
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
        });
  }

  /**
   * Runs the <code>suppliers</code> asynchronously and in sequence using the
   * <code>ForkJoinPool.commonPool()</code>.
   * @param suppliers the given suppliers.
   * @param <T> the element type.
   * @return the stream of generated elements.
   */
  public static <T> CompletionStage<Stream<T>> supplyAsyncStream(
      final Stream<Supplier<T>> suppliers) {
    return supplyAsyncStream(suppliers, commonPool());
  }

  /**
   * Runs the <code>suppliers</code> asynchronously and in sequence.
   * @param suppliers the given suppliers.
   * @param executor th executor.
   * @param <T> the element type.
   * @return the stream of generated elements.
   */
  public static <T> CompletionStage<Stream<T>> supplyAsyncStream(
      final Stream<Supplier<T>> suppliers, final Executor executor) {
    return suppliers
        .sequential()
        .reduce(
            supplyAsync(Stream::<T>builder, executor),
            (stage, s) -> stage.thenApplyAsync(builder -> builder.add(s.get()), executor),
            (s1, s2) -> s1)
        .thenApply(Builder::build);
  }

  /**
   * Iterates sequentially until the predicate returns <code>false</code>. The resulting stream
   * starts with <code>seed</code>.
   *
   * @param seed the initial value.
   * @param f the function that calculates the next value.
   * @param p the predicate.
   * @param <T> the value type.
   * @return The generated stream.
   */
  public static <T> Stream<T> takeWhile(
      final T seed, final UnaryOperator<T> f, final Predicate<T> p) {
    return stream(
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
        });
  }

  /**
   * Iterates sequentially until the predicate returns <code>false</code>. The resulting stream
   * starts with the result of <code>seed</code>.
   *
   * @param stream the stream from which values are taken to drive the generated stream.
   * @param seed the function to generate the initial value. If <code>stream</code> is empty this
   *     function will nit be called.
   * @param f the function that calculates the next value. If <code>stream</code> is empty this *
   *     function will nit be called.
   * @param p the predicate.
   * @param <T> the value type.
   * @return The generated stream.
   */
  public static <T, U> Stream<T> takeWhile(
      final Stream<U> stream,
      final Function<U, T> seed,
      final BiFunction<T, U, T> f,
      final Predicate<T> p) {
    return stream(
        new Iterator<T>() {
          private Iterator<U> i = stream.iterator();
          private T current = i.hasNext() ? seed.apply(i.next()) : null;
          private boolean ok;

          @Override
          public boolean hasNext() {
            ok = current != null && i.hasNext() && p.test(current);

            return ok;
          }

          @Override
          public T next() {
            if (!ok) {
              throw new NoSuchElementException();
            }

            final T result = current;

            current = i.hasNext() ? f.apply(current, i.next()) : null;

            return result;
          }
        });
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
  public static <T, U> Stream<Pair<T, U>> zip(final Stream<T> s1, final Stream<U> s2) {
    return stream(
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
        });
  }

  private static class RangeIterator implements Iterator<Number> {
    private final UnaryOperator<Long> step;
    private final Function<Number, Boolean> test;
    private long index;

    private RangeIterator(final Number from, final Number to, final boolean inclusive) {
      final Function<Number, Boolean> less =
          inclusive
              ? (i -> i.longValue() + 1 <= to.longValue())
              : (i -> i.longValue() + 1 < to.longValue());
      final Function<Number, Boolean> more =
          inclusive
              ? (i -> i.longValue() - 1 >= to.longValue())
              : (i -> i.longValue() - 1 > to.longValue());

      this.test = from.longValue() < to.longValue() ? less : more;
      this.step = from.longValue() < to.longValue() ? (i -> i + 1) : (i -> i - 1);
      this.index = from.longValue() + (from.longValue() < to.longValue() ? -1 : +1);
    }

    public boolean hasNext() {
      return test.apply(index);
    }

    public Long next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      index = step.apply(index);

      return index;
    }
  }
}
