package net.pincette.function;

/**
 * A function interface that allows lambda expressions to throw an exception.
 *
 * @author Werner Donn\u00e9
 */

@FunctionalInterface
public interface FunctionWithException<T, R>

{

  public static <T> FunctionWithException<T, T>
  identity() {
    return a -> a;
  }

  @SuppressWarnings("squid:S00112")
  public R apply(T t) throws Exception;

  default <V> FunctionWithException<T, V>
  andThen(final FunctionWithException<? super R, ? extends V> after) {
    return a -> after.apply(this.apply(a));
  }

  default <V> FunctionWithException<V, R>
  compose(final FunctionWithException<? super V, ? extends T> before) {
    return a -> this.apply(before.apply(a));
  }

} // FunctionWithException
