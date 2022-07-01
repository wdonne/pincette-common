package net.pincette.function;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * An interface to supply optionals.
 *
 * @param <T> the value type.
 * @author Werner Donn\u00e9
 * @since 2.0.2
 */
public interface OptionalSupplier<T> extends Supplier<Optional<T>> {}
