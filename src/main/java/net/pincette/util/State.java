package net.pincette.util;

/**
 * This can be used to manage local state in a function in a context with a lot of lambda's.
 *
 * @param <T> the value type.
 * @author Werner Donné
 * @since 1.7.6
 */
public class State<T> {
  private T value;

  public State() {
    this(null);
  }

  public State(final T value) {
    this.value = value;
  }

  public T get() {
    return value;
  }

  public T set(final T value) {
    this.value = value;

    return value;
  }
}
