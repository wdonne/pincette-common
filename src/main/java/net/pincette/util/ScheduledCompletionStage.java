package net.pincette.util;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.ForkJoinPool.commonPool;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Starts a completable future after a delay without blocking. It uses one timer thread to complete
 * the pending futures.
 *
 * @author Werner Donné
 */
public class ScheduledCompletionStage {
  private static final Timer TIMER = new Timer("ScheduledCompletionStage", true);

  private ScheduledCompletionStage() {}

  public static <T> CompletionStage<T> composeAsyncAfter(
      final Supplier<CompletionStage<T>> supplier, final Duration delay) {
    return runAsyncAfter(supplier, delay);
  }

  private static <T> CompletionStage<T> runAsyncAfter(
      final Supplier<CompletionStage<T>> supplier, final Duration delay) {
    final CompletableFuture<Boolean> future = new CompletableFuture<>();

    TIMER.schedule(
        new TimerTask() {
          @Override
          public void run() {
            future.complete(true);
          }
        },
        delay.toMillis());

    return future.thenComposeAsync(r -> supplier.get());
  }

  public static CompletionStage<Void> runAsyncAfter(final Runnable runnable, final Duration delay) {
    return runAsyncAfter(runnable, delay, commonPool());
  }

  public static CompletionStage<Void> runAsyncAfter(
      final Runnable runnable, final Duration delay, final Executor executor) {
    return supplyAsyncAfter(
        () -> {
          runnable.run();
          return null;
        },
        delay,
        executor);
  }

  public static <T> CompletionStage<T> supplyAsyncAfter(
      final Supplier<T> supplier, final Duration delay) {
    return supplyAsyncAfter(supplier, delay, commonPool());
  }

  public static <T> CompletionStage<T> supplyAsyncAfter(
      final Supplier<T> supplier, final Duration delay, final Executor executor) {
    return runAsyncAfter(() -> supplyAsync(supplier, executor), delay);
  }
}
