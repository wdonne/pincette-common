package net.pincette.function;

/**
 * A supplier interface that allows lambda expressions to throw an exception.
 * @author Werner Donn\u00e9
 */

@FunctionalInterface
public interface SupplierWithException<T>

{

  public T get () throws Exception;

} // SupplierWithException
