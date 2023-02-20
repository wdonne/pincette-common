package net.pincette.util;

import static net.pincette.util.Util.with;

import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This deque offers callbacks for notifying you about the additional and removal of elements.
 *
 * @param <E> the value type.
 * @author Werner Donné
 * @since 2.3
 */
public class NotifyingDeque<E> extends ConcurrentLinkedDeque<E> {
  private final transient Consumer<Deque<E>> onAdd;
  private final transient Consumer<Deque<E>> onRemove;

  /**
   * The constructor with the two callbacks.
   *
   * @param onAdd the callback that is called after elements have been added. It may be <code>null
   *     </code>.
   * @param onRemove the callback that is called after elements have been removed. It may be <code>
   *     null</code>.
   */
  public NotifyingDeque(final Consumer<Deque<E>> onAdd, final Consumer<Deque<E>> onRemove) {
    this.onAdd = onAdd;
    this.onRemove = onRemove;
  }

  @Override
  public boolean add(final E e) {
    return wrapAdd(e, super::add);
  }

  @Override
  public boolean addAll(final Collection<? extends E> c) {
    return wrapAdd(c, super::addAll);
  }

  @Override
  public void addFirst(final E e) {
    wrapAdd(e, super::addFirst);
  }

  @Override
  public void addLast(final E e) {
    wrapAdd(e, super::addLast);
  }

  @Override
  public boolean offer(final E e) {
    return wrapAdd(e, super::offer);
  }

  @Override
  public boolean offerFirst(final E e) {
    return wrapAdd(e, super::offerFirst);
  }

  @Override
  public boolean offerLast(final E e) {
    return wrapAdd(e, super::offerLast);
  }

  @Override
  public E pop() {
    return wrapRemove(super::pop);
  }

  @Override
  public void push(final E e) {
    wrapAdd(e, super::push);
  }

  @Override
  public E remove() {
    return wrapRemove(super::remove);
  }

  @Override
  public boolean remove(final Object o) {
    return wrapRemove(o, (Function<Object, Boolean>) super::remove);
  }

  @Override
  public boolean removeAll(final Collection<?> c) {
    return wrapRemove(c, super::removeAll);
  }

  @Override
  public E removeFirst() {
    return wrapRemove(super::removeFirst);
  }

  @Override
  public boolean removeFirstOccurrence(final Object o) {
    return wrapRemove(o, super::removeFirstOccurrence);
  }

  @Override
  public E removeLast() {
    return wrapRemove(super::removeLast);
  }

  @Override
  public boolean removeLastOccurrence(final Object o) {
    return wrapRemove(o, super::removeLastOccurrence);
  }

  @Override
  public boolean retainAll(final Collection<?> c) {
    return wrapRemove(c, super::retainAll);
  }

  private <T, R> R wrap(final T v, final Function<T, R> fn, final Consumer<Deque<E>> notify) {
    return wrap(() -> fn.apply(v), notify);
  }

  private <T> void wrap(final T v, final Consumer<T> fn, final Consumer<Deque<E>> notify) {
    fn.accept(v);

    if (notify != null) {
      notify.accept(this);
    }
  }

  private <T> T wrap(final Supplier<T> fn, final Consumer<Deque<E>> notify) {
    return with(
        fn,
        r -> {
          if (notify != null) {
            notify.accept(this);
          }

          return r;
        });
  }

  private <T, R> R wrapAdd(final T v, final Function<T, R> fn) {
    return wrap(v, fn, onAdd);
  }

  private <T> void wrapAdd(final T v, final Consumer<T> fn) {
    wrap(v, fn, onAdd);
  }

  private <T, R> R wrapRemove(final T v, final Function<T, R> fn) {
    return wrap(v, fn, onRemove);
  }

  private <T> T wrapRemove(final Supplier<T> fn) {
    return wrap(fn, onRemove);
  }
}
