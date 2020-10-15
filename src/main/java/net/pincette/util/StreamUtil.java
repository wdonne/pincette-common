package net.pincette.util;

import static java.lang.Integer.max;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.stream.Collectors.toList;
import static net.pincette.util.Collections.shiftDown;
import static net.pincette.util.Pair.pair;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
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
   * Runs the <code>stages</code> in sequence using the <code>ForkJoinPool.commonPool()</code>.
   *
   * @param stages the given completion stages.
   * @param <T> the element type.
   * @return the stream of generated elements.
   */
  public static <T> CompletionStage<Stream<T>> composeAsyncStream(
      final Stream<CompletionStage<T>> stages) {
    return composeAsyncStream(stages, commonPool());
  }

  /**
   * Runs the <code>stages</code> in sequence.
   *
   * @param stages the given completion stages.
   * @param executor th executor.
   * @param <T> the element type.
   * @return the stream of generated elements.
   */
  public static <T> CompletionStage<Stream<T>> composeAsyncStream(
      final Stream<CompletionStage<T>> stages, final Executor executor) {
    return stages
        .sequential()
        .reduce(
            supplyAsync(Stream::<T>builder, executor),
            (stage, s) -> stage.thenComposeAsync(builder -> s.thenApply(builder::add), executor),
            (s1, s2) -> s1)
        .thenApply(Builder::build);
  }

  /**
   * Create an iterable of a stream using its iterator.
   *
   * @param stream the given stream.
   * @param <T> the value type.
   * @return The generated iterable.
   * @since 1.7.3
   */
  public static <T> Iterable<T> iterable(final Stream<T> stream) {
    return stream::iterator;
  }

  /**
   * Returns the last element of a stream.
   *
   * @param stream the given stream.
   * @param <T> the element type.
   * @return the last element.
   */
  public static <T> Optional<T> last(final Stream<T> stream) {
    return ofNullable(stream.sequential().reduce(null, (result, element) -> element));
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
    return rangeExclusive(from, to, 1);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is excluded.
   * @param step the positive distance for the iteration.
   * @return the integer stream.
   * @since 1.6.8
   */
  public static Stream<Long> rangeExclusive(final long from, final long to, final int step) {
    return stream(new RangeIterator(from, to, step, false)).map(Number::longValue);
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
    return rangeExclusive(from, to, 1);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is excluded.
   * @param step the positive distance for the iteration.
   * @return the integer stream.
   * @since 1.6.8
   */
  public static Stream<Integer> rangeExclusive(final int from, final int to, final int step) {
    return stream(new RangeIterator(from, to, step, false)).map(Number::intValue);
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
    return rangeInclusive(from, to, 1);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is included.
   * @param step the positive distance for the iteration.
   * @return the integer stream.
   * @since 1.6.8
   */
  public static Stream<Long> rangeInclusive(final long from, final long to, final int step) {
    return stream(new RangeIterator(from, to, step, true)).map(Number::longValue);
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
    return rangeInclusive(from, to, 1);
  }

  /**
   * Produces a sequential integer stream. If <code>from</code> is larger than <code>to</code> then
   * the stream will count down.
   *
   * @param from first value of the range.
   * @param to last value of the range, which is included.
   * @param step the positive distance for the iteration.
   * @return the integer stream.
   * @since 1.6.8
   */
  public static Stream<Integer> rangeInclusive(final int from, final int to, final int step) {
    return stream(new RangeIterator(from, to, step, true)).map(Number::intValue);
  }

  /**
   * Reduces the operator stream to a value where the seeded value falls through until a predicate
   * is met or the end is reached.
   *
   * @param seed the seed value.
   * @param operators the stream of operators.
   * @param until the predicate, which stops the evaluation.
   * @param <T> the value type.
   * @return The reduced value.
   * @since 1.6.7
   */
  public static <T> T reduceUntil(
      final T seed, final Stream<UnaryOperator<T>> operators, final Predicate<T> until) {
    return operators.reduce(seed, (v, o) -> until.test(v) ? v : o.apply(v), (v1, v2) -> v1);
  }

  /**
   * Reduces the operator stream to a completion stage chain where the seeded value falls through
   * until a predicate is met or the end is reached.
   *
   * @param seed the function that provides the seed value.
   * @param operators the stream of operators.
   * @param until the predicate, which stops the evaluation.
   * @param <T> the value type.
   * @return The reduced value.
   * @since 1.6.7
   */
  public static <T> CompletionStage<T> reduceUntilAsync(
      final Supplier<CompletionStage<T>> seed,
      final Stream<Function<T, CompletionStage<T>>> operators,
      final Predicate<T> until) {
    return operators.reduce(
        seed.get(),
        (s, o) -> s.thenComposeAsync(v -> until.test(v) ? completedFuture(v) : o.apply(v)),
        (s1, s2) -> s1);
  }

  /**
   * Returns a stream of sliding windows over <code>stream</code>. Only windows of exactly <code>
   * windowSize</code> are returned.
   *
   * @param stream the given stream.
   * @param windowSize the size of the returned windows.
   * @param <T> the element type.
   * @return The new stream.
   * @since 1.7
   */
  public static <T> Stream<List<T>> slide(final Stream<T> stream, final int windowSize) {
    return stream(
        new Iterator<List<T>>() {
          final Iterator<T> iterator = stream.iterator();
          List<T> window;

          private List<T> initialWindow() {
            return rangeExclusive(0, windowSize)
                .map(i -> iterator.hasNext() ? iterator.next() : null)
                .filter(Objects::nonNull)
                .collect(toList());
          }

          @Override
          public boolean hasNext() {
            window = window == null ? initialWindow() : newWindow();

            return window.size() == windowSize;
          }

          private List<T> newWindow() {
            return iterator.hasNext() ? shiftDown(window, 1, iterator.next()) : emptyList();
          }

          @Override
          public List<T> next() {
            if (window == null || window.size() < windowSize) {
              throw new NoSuchElementException();
            }

            return window;
          }
        });
  }

  public static <T> Stream<T> stream(final Iterator<T> iterator) {
    return StreamSupport.stream(
        spliteratorUnknownSize(iterator, ORDERED | NONNULL | IMMUTABLE), false);
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
   * Runs the <code>suppliers</code> asynchronously and in sequence using the <code>
   * ForkJoinPool.commonPool()</code>.
   *
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
   *
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
   * @param <U> the value type of the driving stream.
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
            ok = current != null && p.test(current);

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
    private final UnaryOperator<Long> doStep;
    private final Predicate<Number> test;
    private long index;

    private RangeIterator(
        final Number from, final Number to, final int step, final boolean inclusive) {
      final int realStep = max(1, step);
      final Predicate<Number> less =
          inclusive
              ? (i -> i.longValue() + realStep <= to.longValue())
              : (i -> i.longValue() + realStep < to.longValue());
      final Predicate<Number> more =
          inclusive
              ? (i -> i.longValue() - realStep >= to.longValue())
              : (i -> i.longValue() - realStep > to.longValue());

      this.test = from.longValue() < to.longValue() ? less : more;
      this.doStep = from.longValue() < to.longValue() ? (i -> i + realStep) : (i -> i - realStep);
      this.index = from.longValue() + (from.longValue() < to.longValue() ? -realStep : realStep);
    }

    public boolean hasNext() {
      return test.test(index);
    }

    public Long next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      index = doStep.apply(index);

      return index;
    }
  }
}
