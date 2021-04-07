package net.pincette.util;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This lets you chain a number of predicates with suppliers. The value of the first supplier with a
 * matching predicate is the result returned by the <code>get</code> method. It can be used like
 * this:
 *
 * <p>{@code Cases.withValue(v).or(v -> test1).or(v -> test2).get();}
 *
 * @author Werner Donn\u00e9
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

  private Cases<T, U> next(final Predicate<T> predicate, final Supplier<U> supplier) {
    return new Cases<>(value, predicate.test(value) ? supplier.get() : null);
  }

  public Cases<T, U> or(final Predicate<T> predicate, final Supplier<U> supplier) {
    return result != null ? this : next(predicate, supplier);
  }
}
