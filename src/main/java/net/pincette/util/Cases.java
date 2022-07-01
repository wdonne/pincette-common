package net.pincette.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This lets you chain a number of predicates with functions. The value of the first function with a
 * matching predicate is the result returned by the <code>get</code> method. It can be used like
 * this:
 *
 * <p>{@code Cases.<T, U>withValue(v).or(v -> test1, v -> v1).or(v -> test2, v -> v2).get();}
 *
 * @author Werner Donn\u00e9
 * @since 1.8
 */
public class Cases<T, U> {
  private final U result;
  private final T value;

  private Cases(final T value, final U result) {
    this.value = value;
    this.result = result;
  }

  public static <T, U> Cases<T, U> withValue(final T value) {
    return new Cases<>(value, null);
  }

  public Optional<U> get() {
    return Optional.ofNullable(result);
  }

  private Cases<T, U> next(final Predicate<T> predicate, final Function<T, U> fn) {
    return new Cases<>(value, predicate.test(value) ? fn.apply(value) : null);
  }

  private <V> Cases<T, U> next(final Function<T, Optional<V>> get, final Function<V, U> fn) {
    return get.apply(value)
        .map(fn)
        .map(v -> new Cases<>(value, v))
        .orElseGet(() -> new Cases<>(value, null));
  }

  public Cases<T, U> or(final Predicate<T> predicate, final Function<T, U> fn) {
    return result != null ? this : next(predicate, fn);
  }

  public <V> Cases<T, U> orGet(final Function<T, Optional<V>> get, final Function<V, U> fn) {
    return result != null ? this : next(get, fn);
  }
}
