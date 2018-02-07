package net.pincette.util;

import java.util.ArrayList;

public class ArrayBuilder<T> {

  private final ArrayList<T> list;

  /**
   * Creates a builder with an initial capacity of ten.
   */

  public ArrayBuilder() {
    list = new ArrayList<>(10);
  }

  public ArrayBuilder(final int initialCapacity) {
    list = new ArrayList<>(initialCapacity);
  }

  public ArrayBuilder<T>
  add(final T object) {
    list.add(object);

    return this;
  }

  public int
  size() {
    return list.size();
  }

  public T[]
  toArray() {
    return (T[]) list.toArray();
  }

} // ArrayBuilder
