package net.pincette.util;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.pincette.util.Util.tryToGetRethrow;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.pincette.function.FunctionWithException;
import net.pincette.function.SupplierWithException;

/**
 * Chains asynchronous updates to mutable objects.
 *
 * @param <T> the object type.
 * @author Werner Donn\u00e9
 * @since 1.9
 */
public class AsyncBuilder<T> {
  private final CompletionStage<Optional<T>> object;

  private AsyncBuilder(final CompletionStage<Optional<T>> object) {
    this.object = object;
  }

  public static <T> AsyncBuilder<T> create(final SupplierWithException<T> supplier) {
    return new AsyncBuilder<>(
        tryToGetRethrow(supplier)
            .map(Optional::of)
            .map(CompletableFuture::completedFuture)
            .orElse(null));
  }

  private static <T> CompletionStage<Optional<T>> next(
      final CompletionStage<Optional<T>> object,
      final FunctionWithException<T, CompletionStage<Optional<T>>> set) {
    return object.thenComposeAsync(
        obj ->
            obj.flatMap(o -> tryToGetRethrow(() -> set.apply(o)))
                .orElseGet(() -> completedFuture(Optional.empty())));
  }

  public CompletionStage<Optional<T>> build() {
    return object;
  }

  /**
   * Calls <code>set</code> when previous updates have completed, only when the latter didn't return
   * an empty result.
   *
   * @param set the function that updates the mutable object.
   * @return A builder with the additional update completion stage.
   */
  public AsyncBuilder<T> update(final FunctionWithException<T, CompletionStage<Optional<T>>> set) {
    return new AsyncBuilder<>(next(object, set));
  }
}
