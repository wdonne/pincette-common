package net.pincette.function;

/**
 * An consumer interface that allows lambda expressions to throw an exception.
 * @author Werner Donn\u00e9
 */

@FunctionalInterface
public interface ConsumerWithException<T>

{

  public void accept (T t) throws Exception;



  default public ConsumerWithException<T>
  andThen(ConsumerWithException<T> after)
  {
    return a -> {this.accept(a); after.accept(a);};
  }

} // ConsumerWithException
