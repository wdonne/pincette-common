package net.pincette.function;

import java.util.function.Supplier;

/**
 * Allows to interleave statements in expressions. You can run it like this:
 *
 * <p>{@code final int i = SideEffect.<Integer>run(() -> doSomething()).andThenGet(() -> 3); }
 *
 * @author Werner Donn\u00e9
 */
@FunctionalInterface
public interface SideEffect<T> extends Runnable {

  /**
   * Runs <code>runnable</code> and then returns a <code>SideEffect</code> on which the <code>
   * andThenGet</code> method can be called.
   *
   * @param sideEffect the side effect to be run.
   * @param <T> the type of the value that will eventually be returned.
   * @return The <code>SideEffect</code> object.
   */
  static <T> SideEffect<T> run(Runnable sideEffect) {
    return sideEffect::run;
  }

  /**
   * This method should be called in order for the side effect to execute.
   *
   * @param supplier the function that produces the final result.
   * @return The result of <code>supplier</code>.
   */
  default T andThenGet(Supplier<T> supplier) {
    run();

    return supplier.get();
  }
}
