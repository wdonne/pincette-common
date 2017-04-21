package net.pincette.util;

import net.pincette.function.ConsumerWithException;



/**
 * When the collection is closed all the containing elements are closed too.
 * @author Werner Donn\u00e9
 */

public interface AutoCloseCollection<T> extends AutoCloseable

{

  /**
   * When the collection is closed <code>close</code> will be called with
   * <code>element</code>.
   * @param element the element to be added.
   * @param close the function that will be called during the auto close phase.
   * @param <U> the type of the element.
   * @return The added element.
   */

  public <U> U add (U element, ConsumerWithException<U> close);

  /**
   * The implementation should provide a default close function.
   * @param element the element to be added.
   * @return The added element.
   */

  public T add (T element);

} // AutoCloseCollection
