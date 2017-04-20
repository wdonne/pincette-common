package net.pincette.function;

import java.util.function.Supplier;



/**
 * Allows to interleave statements in expressions.
 * @author Werner Donn\u00e9
 */

@FunctionalInterface
public interface SideEffect<T> extends Runnable

{

  default public T
  andThenGet(Supplier<T> supplier)
  {
    run();

    return supplier.get();
  }



  /**
   * Runs <code>runnable</code> and then runs <code>supplier</code>.
   */

  public static <T> SideEffect<T>
  run(Runnable sideEffect)
  {
    return () -> sideEffect.run();
  }

} // SideEffect
