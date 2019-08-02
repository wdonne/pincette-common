package net.pincette.rs;

import java.util.function.Function;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Transforms a reactive stream and adds a start, end and separator value to it.
 *
 * @param <T> the type of the incoming values.
 * @param <R> the type of the outgoing values.
 * @author Werner Donn\u00e9
 * @deprecated Use pincette-rs.
 */
@Deprecated
public class Intersperse<T, R> implements Processor<T, R> {
  private final R end;
  private final R separator;
  private final R start;
  private final Function<T, R> transform;
  private boolean first;
  private Subscriber<? super R> subscriber;
  private Subscription subscription;

  public Intersperse(
      final R start, final R separator, final R end, final Function<T, R> transform) {
    this.start = start;
    this.separator = separator;
    this.end = end;
    this.transform = transform;
  }

  public void onComplete() {
    if (subscriber != null) {
      subscriber.onNext(end);
      subscriber.onComplete();
    }
  }

  public void onError(final Throwable t) {
    if (subscriber != null) {
      subscriber.onError(t);
    }
  }

  public void onNext(final T value) {
    if (subscriber != null) {
      if (first) {
        first = false;
      } else {
        subscriber.onNext(separator);
      }

      subscriber.onNext(transform.apply(value));
    }
  }

  public void onSubscribe(final Subscription subscription) {
    this.subscription = subscription;
    subscription.request(1);
  }

  public void subscribe(final Subscriber<? super R> subscriber) {
    this.subscriber = subscriber;

    if (subscriber != null) {
      subscriber.onSubscribe(new Backpressure());
      subscriber.onNext(start);
      first = true;
    }
  }

  private class Backpressure implements Subscription {
    public void cancel() {
      if (subscription != null) {
        subscription.cancel();
      }
    }

    public void request(final long l) {
      if (subscription != null) {
        subscription.request(l);
      }
    }
  }
}
