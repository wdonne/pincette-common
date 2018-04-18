package net.pincette.function;

/**
 * A runnable interface that allows lambda expressions to throw an exception.
 *
 * @author Werner Donn\u00e9
 */
@FunctionalInterface
public interface RunnableWithException {

  @SuppressWarnings("squid:S00112")
  void run() throws Exception;
}
