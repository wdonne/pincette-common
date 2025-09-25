package net.pincette.util;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.pincette.function.BiConsumerWithException;
import net.pincette.function.ConsumerWithException;
import net.pincette.function.SupplierWithException;

/**
 * Chains updates to mutable objects.
 *
 * @param <T> the object type.
 * @author Werner Donné
 */
public class Builder<T> {
  private final T object;

  private Builder(final T object) {
    this.object = object;
  }

  public static <T> Builder<T> create(final SupplierWithException<T> supplier) {
    return new Builder<>(tryToGetRethrow(supplier).orElse(null));
  }

  public T build() {
    return object;
  }

  public Builder<T> update(final ConsumerWithException<T> set) {
    tryToDoRethrow(() -> set.accept(object));

    return this;
  }

  public Builder<T> updateIf(final Predicate<T> predicate, final ConsumerWithException<T> set) {
    return predicate.test(object) ? update(set) : this;
  }

  public <U> Builder<T> updateIf(
      final Supplier<Optional<U>> value, final BiConsumerWithException<T, U> set) {
    return value.get().map(v -> update(o -> set.accept(o, v))).orElse(this);
  }
}
