package net.pincette.function;

/**
 * @author Werner Donn\u00e9
 */

@FunctionalInterface
public interface SupplierWithException<T>

{

  public T get () throws Exception;

} // SupplierWithException
