package net.pincette.util;


import net.pincette.function.RunnableWithException;

import static net.pincette.util.Util.tryToDoRethrow;



/**
 * Lets you write conditional statements without blocks. which can be useful
 * for consumers and runnables. Exampe:
 * <p>
 * {@code
 * () ->
 * when(<condition>).run(() -> <do something>).orElse(() -> <do something else>)
 * }
 * </p>
 * @author Werner Donn\u00e9
 */

public class When

{

  final private boolean condition;



  private
  When(final boolean condition)
  {
    this.condition = condition;
  }



  public void
  orElse(final RunnableWithException runnable)
  {
    if (!condition)
    {
      tryToDoRethrow(runnable);
    }
  }



  public When
  run(final RunnableWithException runnable)
  {
    if (condition)
    {
      tryToDoRethrow(runnable);
    }

    return this;
  }



  public static When
  when(final boolean condition)
  {
    return new When(condition);
  }

} // When
