package net.pincette.function;

/**
 * An bi-consumer interface that allows lambda expressions to throw an exception.
 *
 * @author Werner Donn\u00e9
 */
@FunctionalInterface
public interface BiConsumerWithException<T, U> {

  @SuppressWarnings("squid:S00112")
  void accept(T t, U u) throws Exception;

  default BiConsumerWithException<T, U> andThen(BiConsumerWithException<T, U> after) {
    return (t, u) -> {
      this.accept(t, u);
      after.accept(t, u);
    };
  }
}
