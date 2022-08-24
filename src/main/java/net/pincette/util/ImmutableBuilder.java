package net.pincette.util;

import static net.pincette.util.Util.tryToGetRethrow;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.pincette.function.BiFunctionWithException;
import net.pincette.function.SupplierWithException;
import net.pincette.function.UnaryOperatorWithException;

/**
 * Chains immutable object creation with optional conditions.
 *
 * @param <T> the object type.
 * @author Werner Donn\u00e9
 * @since 2.0.3
 */
public class ImmutableBuilder<T> {
  private T object;

  private ImmutableBuilder(final T object) {
    this.object = object;
  }

  public static <T> ImmutableBuilder<T> create(final SupplierWithException<T> supplier) {
    return new ImmutableBuilder<>(tryToGetRethrow(supplier).orElse(null));
  }

  public T build() {
    return object;
  }

  public ImmutableBuilder<T> update(final UnaryOperatorWithException<T> set) {
    object = tryToGetRethrow(() -> set.apply(object)).orElse(object);

    return this;
  }

  public ImmutableBuilder<T> updateIf(
      final Predicate<T> predicate, final UnaryOperatorWithException<T> set) {
    return predicate.test(object) ? update(set) : this;
  }

  public <U> ImmutableBuilder<T> updateIf(
      final Supplier<Optional<U>> value, final BiFunctionWithException<T, U, T> set) {
    return value.get().map(v -> update(o -> set.apply(o, v))).orElse(this);
  }
}
