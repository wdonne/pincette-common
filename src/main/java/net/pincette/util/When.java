package net.pincette.util;

import static net.pincette.util.Util.tryToDoRethrow;

import net.pincette.function.RunnableWithException;

/**
 * Lets you write conditional statements without blocks. which can be useful for consumers and
 * runnables. Exampe:
 *
 * <p>{@code () -> when(<condition>).run(() -> <do something>).orElse(() -> <do something else>) }
 *
 * @author Werner Donné
 */
public class When {
  private final boolean condition;

  private When(final boolean condition) {
    this.condition = condition;
  }

  public static When when(final boolean condition) {
    return new When(condition);
  }

  public void orElse(final RunnableWithException runnable) {
    if (!condition) {
      tryToDoRethrow(runnable);
    }
  }

  public When run(final RunnableWithException runnable) {
    if (condition) {
      tryToDoRethrow(runnable);
    }

    return this;
  }
}
