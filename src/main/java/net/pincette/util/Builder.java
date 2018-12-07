package net.pincette.util;

import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.util.function.Predicate;
import net.pincette.function.ConsumerWithException;
import net.pincette.function.SupplierWithException;

/**
 * Chains updates to mutable objects.
 *
 * @param <T> the object type.
 * @author Werner Donn\u00e9
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
}
