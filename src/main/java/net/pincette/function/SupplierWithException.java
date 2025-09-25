package net.pincette.function;

/**
 * A supplier interface that allows lambda expressions to throw an exception.
 *
 * @author Werner Donné
 */
@FunctionalInterface
public interface SupplierWithException<T> {
  @SuppressWarnings("squid:S00112")
  T get() throws Exception;
}
