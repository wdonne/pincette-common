package net.pincette.function;

/**
 * A bi-function interface that allows lambda expressions to throw an exception.
 *
 * @author Werner Donné
 */
@FunctionalInterface
public interface BiFunctionWithException<T, U, R> {
  @SuppressWarnings("squid:S00112")
  public R apply(T t, U u) throws Exception;

  default <V> BiFunctionWithException<T, U, V> andThen(
      final FunctionWithException<? super R, ? extends V> after) {
    return (t, u) -> after.apply(this.apply(t, u));
  }
}
