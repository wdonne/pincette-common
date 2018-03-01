package net.pincette.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetBuilder<E> {

  private final Set<E> set;

  /**
   * Builds a copy of <code>set</code>.
   *
   * @param set the copied set.
   */
  public SetBuilder(final Set<? extends E> set) {
    this.set = new HashSet<>(set);
  }

  public Set<E>
  build() {
    return set;
  }

  public SetBuilder<E>
  add(final E e) {
    set.add(e);

    return this;
  }

  public SetBuilder<E>
  addAll(final Collection<? extends E> c) {
    set.addAll(c);

    return this;
  }

  public SetBuilder<E>
  remove(final Object o) {
    set.remove(o);

    return this;
  }

  public SetBuilder<E>
  removeAll(final Collection<?> c) {
    set.removeAll(c);

    return this;
  }

  public SetBuilder<E>
  retainAll(Collection<?> c) {
    set.retainAll(c);

    return this;
  }

} // SetBuilder
