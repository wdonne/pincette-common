package net.pincette.util;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * This lets you chain a number of lambda expressions. The execution stops at the first expression
 * that yields a value. It can be used like this:
 *
 * <p>{@code Or.tryWith(lamba1).or(lambda2).or(lambda3).get();}
 *
 * @author Werner Donn\u00e9
 */
public class Or<T> {
  private final T result;

  private Or(final Supplier<T> supplier) {
    this.result = supplier.get();
  }

  public static <T> Or<T> tryWith(final Supplier<T> supplier) {
    return new Or<>(supplier);
  }

  public Optional<T> get() {
    return Optional.ofNullable(result);
  }

  public Or<T> or(final Supplier<T> supplier) {
    return result != null ? this : new Or<>(supplier);
  }
}
