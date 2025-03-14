package net.pincette.util;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An simple args utility for main methods.
 *
 * @author Werner Donné
 */
public class ArgsBuilder {
  private final Map<String, String> args = new HashMap<>();
  private String pending;

  /**
   * Adds <code>arg</code> as a key, with the empty string as the value, if there is no pending arg
   * and as a value for the pending key otherwise.
   *
   * @param arg the key or the value.
   * @return The builder.
   */
  public ArgsBuilder add(final String arg) {
    if (pending != null) {
      args.put(pending, arg);
      pending = null;
    } else {
      args.put(arg, "");
    }

    return this;
  }

  /**
   * Expects another arg, which will be the value for <code>arg</code>.
   *
   * @param arg the key.
   * @return The builder.
   */
  public ArgsBuilder addPending(final String arg) {
    pending = arg;

    return this;
  }

  /**
   * Return an empty optional is there is a pending arg, a map otherwise.
   *
   * @return The map.
   */
  public Optional<Map<String, String>> build() {
    return pending != null ? empty() : of(args);
  }

  /**
   * Indicates if a pending arg has been added last.
   *
   * @return
   */
  public boolean hasPending() {
    return pending != null;
  }
}
