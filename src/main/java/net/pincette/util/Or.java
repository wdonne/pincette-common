package net.pincette.util;

import java.util.Optional;
import java.util.function.Supplier;



/**
 * @author Werner Donn\u00e9
 */

public class Or<T>

{

  final private T result;



  private
  Or(final Supplier<T> supplier)
  {
    this.result = supplier.get();
  }



  public Optional<T>
  get()
  {
    return Optional.ofNullable(result);
  }



  public Or<T>
  or(final Supplier<T> supplier)
  {
    return result != null ? this : new Or<T>(supplier);
  }



  public static <T> Or<T>
  tryWith(final Supplier<T> supplier)
  {
    return new Or<T>(supplier);
  }

} // Or
