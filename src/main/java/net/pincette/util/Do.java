package net.pincette.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.pincette.function.SideEffect;

/**
 * This lets you chain a number of predicates with consumers. It falls through the <code>or</code>
 * calls until a predicate returns <code>true</code>.
 *
 * <p>{@code Do.<T>withValue(v).or(v -> test1, v -> {}).or(v -> test2, v -> {});}
 *
 * @author Werner Donné
 * @since 1.9.2
 */
public class Do<T> {
  private final boolean done;
  private final T value;

  private Do(final T value, final boolean done) {
    this.value = value;
    this.done = done;
  }

  public static <T> Do<T> withValue(final T value) {
    return new Do<>(value, false);
  }

  private Do<T> next(final Predicate<T> predicate, final Consumer<T> fn) {
    return predicate.test(value)
        ? SideEffect.<Do<T>>run(() -> fn.accept(value)).andThenGet(() -> new Do<>(value, true))
        : new Do<>(value, false);
  }

  private <U> Do<T> next(final Function<T, Optional<U>> get, final Consumer<U> fn) {
    return get.apply(value)
        .map(v -> SideEffect.<Do<T>>run(() -> fn.accept(v)).andThenGet(() -> new Do<>(value, true)))
        .orElseGet(() -> new Do<>(value, false));
  }

  public Do<T> or(final Predicate<T> predicate, final Consumer<T> fn) {
    return done ? this : next(predicate, fn);
  }

  public <U> Do<T> orGet(final Function<T, Optional<U>> get, final Consumer<U> fn) {
    return done ? this : next(get, fn);
  }
}
