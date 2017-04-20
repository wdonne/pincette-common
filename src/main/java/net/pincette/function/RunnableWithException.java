package net.pincette.function;

/**
 * @author Werner Donn\u00e9
 */

@FunctionalInterface
public interface RunnableWithException

{

  public void run () throws Exception;

} // RunnableWithException
