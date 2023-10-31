package net.pincette.util;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Math.max;
import static java.lang.String.join;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.util.Arrays.fill;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.logging.LogManager.getLogManager;
import static java.util.logging.Logger.getLogger;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.ScheduledCompletionStage.composeAsyncAfter;
import static net.pincette.util.ScheduledCompletionStage.runAsyncAfter;
import static net.pincette.util.ShadowString.shadow;
import static net.pincette.util.StreamUtil.stream;
import static net.pincette.util.StreamUtil.takeWhile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.pincette.function.BiFunctionWithException;
import net.pincette.function.ConsumerWithException;
import net.pincette.function.FunctionWithException;
import net.pincette.function.RunnableWithException;
import net.pincette.function.SideEffect;
import net.pincette.function.SupplierWithException;
import net.pincette.io.EscapedUnicodeFilterReader;
import net.pincette.io.StreamConnector;

/**
 * General purpose utility functions.
 *
 * @author Werner Donn\u00e9
 */
public class Util {
  private static final Pattern EMAIL =
      compile("[\\w.%+\\-]+@[a-zA-Z\\d\\-]+(\\.[a-zA-Z\\d\\-]+)*\\.[a-zA-Z]{2,}");
  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
  private static final Pattern INSTANT =
      compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|\\+00:00)");
  private static final Object VOID = new Object();

  private Util() {}

  /**
   * This returns a function that accumulates the results. Each call will receive the result of the
   * previous call.
   *
   * @param fn the original function.
   * @param initialValue the accumulated value for the first call.
   * @param <T> the value type.
   * @param <R> the result type.
   * @return The generated function.
   * @since 1.6.8
   */
  public static <T, R> Function<T, R> accumulate(
      final BiFunctionWithException<R, T, R> fn, final R initialValue) {
    final State<R> state = new State<>(initialValue);

    return v -> state.set(tryToGetRethrow(() -> fn.apply(state.get(), v)).orElse(null));
  }

  /**
   * Produces a stream with all paths from <code>path</code> up to the root path, which is just the
   * <code>delimiter</code> in case the given path starts with the delimiter. A trailing <code>
   * delimiter</code> will be discarded.
   *
   * @param path the path that will be decomposed.
   * @param delimiter the literal string that separates the path segments.
   * @return The stream with the generated paths.
   */
  public static Stream<String> allPaths(final String path, final String delimiter) {
    final String leading = path.startsWith(delimiter) ? delimiter : "";
    final CharSequence[] segments = getSegments(path, delimiter).toArray(CharSequence[]::new);

    return concat(
        takeWhile(0, i -> i + 1, i -> i < segments.length)
            .map(
                i ->
                    leading
                        + Arrays.stream(segments, 0, segments.length - i)
                            .collect(joining(delimiter))),
        !leading.equals("") ? Stream.of(delimiter) : Stream.empty());
  }

  /**
   * Wraps a resource in an {@link java.lang.AutoCloseable}.
   *
   * @param resource the given resource.
   * @param close the function that is called with the resource.
   * @param <T> the resource type.
   * @return The wrapped resource.
   */
  public static <T> AutoCloseWrapper<T> autoClose(
      final SupplierWithException<T> resource, final ConsumerWithException<T> close) {
    return new AutoCloseWrapper<>(resource, close);
  }

  /**
   * Converts a path with "." and ".." segments into a resolved canonical path.
   *
   * @param path the original path.
   * @param delimiter the path delimiter.
   * @return The canonical path.
   * @since 1.8.1
   */
  public static String canonicalPath(final String path, final String delimiter) {
    return delimiter
        + String.join(
            delimiter,
            segments(path, delimiter)
                .filter(s -> s.length() > 0 && !s.equals("."))
                .reduce(
                    new LinkedList<>(),
                    (l, s) -> {
                      if (s.equals("..")) {
                        l.removeLast();
                      } else {
                        l.add(s);
                      }

                      return l;
                    },
                    (l1, l2) -> l1));
  }

  public static <T, R, V> Function<T, R> compose(
      final Function<T, V> fn1, final Function<V, R> fn2) {
    return fn1.andThen(fn2);
  }

  /**
   * Compresses using GZIP.
   *
   * @param b the array to be compressed.
   * @return The compressed array.
   */
  public static byte[] compress(final byte[] b) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(
        () -> StreamConnector.copy(new ByteArrayInputStream(b), new GZIPOutputStream(out)));

    return out.toByteArray();
  }

  /**
   * Returs a copy of <code>properties</code>.
   *
   * @param properties the given properties.
   * @return The copy of the properties object.
   * @since 1.8
   */
  public static Properties copy(final Properties properties) {
    final Properties result = new Properties();

    properties
        .stringPropertyNames()
        .forEach(key -> result.setProperty(key, properties.getProperty(key)));

    return result;
  }

  /**
   * Returns an iterator over pairs where the first element is the result of the given iterator and
   * the second the zero-based position in the list.
   *
   * @param iterator the given iterator.
   * @param <T> the element type.
   * @return The new iterator.
   */
  public static <T> Iterator<Pair<T, Integer>> countingIterator(final Iterator<T> iterator) {
    return new Iterator<Pair<T, Integer>>() {
      private int count = 0;

      public boolean hasNext() {
        return iterator.hasNext();
      }

      public Pair<T, Integer> next() {
        return pair(iterator.next(), count++);
      }
    };
  }

  /**
   * Decompresses using GZIP.
   *
   * @param b the array to be decompressed.
   * @return The decompressed array.
   */
  public static byte[] decompress(final byte[] b) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(
        () -> StreamConnector.copy(new GZIPInputStream(new ByteArrayInputStream(b)), out));

    return out.toByteArray();
  }

  /**
   * Executes <code>runnable</code> infinitely.
   *
   * @param runnable a function that may throw an exception, which will be rethrown.
   */
  public static void doForever(final RunnableWithException runnable) {
    doForever(runnable, Util::rethrow, null);
  }

  /**
   * Executes <code>runnable</code> infinitely.
   *
   * @param runnable a function that may throw an exception, which will be rethrown.
   * @param interval the time between two calls of <code>runnable</code>. It may be <code>null
   *     </code>.
   * @since 2.3
   */
  public static void doForever(final RunnableWithException runnable, final Duration interval) {
    doForever(runnable, Util::rethrow, interval);
  }

  /**
   * Executes <code>runnable</code> infinitely.
   *
   * @param runnable a function that may throw an exception, which will be rethrown.
   * @param error the function that deals with an exception.
   */
  public static void doForever(
      final RunnableWithException runnable, final Consumer<Exception> error) {
    doForever(runnable, error, null);
  }

  /**
   * Executes <code>runnable</code> infinitely.
   *
   * @param runnable a function that may throw an exception, which will be rethrown.
   * @param error the function that deals with an exception.
   * @param interval the time between two calls of <code>runnable</code>. It may be <code>null
   *     </code>.
   * @since 2.3
   */
  public static void doForever(
      final RunnableWithException runnable,
      final Consumer<Exception> error,
      final Duration interval) {
    tryToDo(
        () -> {
          while (true) {
            runnable.run();

            if (interval != null) {
              sleep(interval.toMillis());
            }
          }
        },
        error);
  }

  /**
   * Executes <code>supplier</code> until it returns <code>true</code>.
   *
   * @param supplier a function that may throw an exception, which will be rethrown.
   * @since 2.0.1
   */
  public static void doUntil(final SupplierWithException<Boolean> supplier) {
    doUntil(supplier, Util::rethrow, null);
  }

  /**
   * Executes <code>supplier</code> until it returns <code>true</code>.
   *
   * @param supplier a function that may throw an exception, which will be rethrown.
   * @param interval the time between two calls of <code>supplier</code>. It may be <code>null
   *     </code>.
   * @since 2.3
   */
  public static void doUntil(
      final SupplierWithException<Boolean> supplier, final Duration interval) {
    doUntil(supplier, Util::rethrow, interval);
  }

  /**
   * Executes <code>supplier</code> until it returns <code>true</code>.
   *
   * @param supplier a function that may throw an exception, which will be rethrown.
   * @param error the function that deals with an exception.
   * @since 2.0.1
   */
  public static void doUntil(
      final SupplierWithException<Boolean> supplier, final Consumer<Exception> error) {
    doUntil(supplier, error, null);
  }

  /**
   * Executes <code>supplier</code> until it returns <code>true</code>.
   *
   * @param supplier a function that may throw an exception, which will be rethrown.
   * @param error the function that deals with an exception.
   * @param interval the time between two calls of <code>supplier</code>. It may be <code>null
   *     </code>.
   * @since 2.3
   */
  public static void doUntil(
      final SupplierWithException<Boolean> supplier,
      final Consumer<Exception> error,
      final Duration interval) {
    tryToDo(
        () -> {
          while (FALSE.equals(supplier.get()))
            ;

          if (interval != null) {
            sleep(interval.toMillis());
          }
        },
        error);
  }

  /**
   * Returns <code>true</code> if the given object equals one of the given values.
   *
   * @param o the given object.
   * @param values the values to compare with.
   * @return The comparison result.
   */
  public static boolean equalsOneOf(final Object o, final Object... values) {
    return Arrays.asList(values).contains(o);
  }

  private static String flushLines(final List<String> buffer) {
    final String result = join("", buffer);

    return SideEffect.<String>run(buffer::clear).andThenGet(() -> result);
  }

  /**
   * Allows to write things like <code>from(value).accept(v -&gt; {...})</code>. This way you don't
   * need to declare a variable for the value.
   *
   * @param value the given value.
   * @param <T> the type of the given value.
   * @return The function that accepts a function to consume the value.
   */
  public static <T> Consumer<ConsumerWithException<T>> from(final T value) {
    return fn -> tryToDoRethrow(() -> fn.accept(value));
  }

  private static Optional<String> getArrayExpression(final String name) {
    return Optional.of(name.indexOf('['))
        .filter(i -> i != -1 && name.charAt(name.length() - 1) == ']')
        .map(i -> name.substring(i + 1, name.length() - 1).trim());
  }

  private static <T> Map<String, Object> getFromList(
      final List<Map<String, Object>> list, final String name, final Function<T, ?> evaluator) {
    return getArrayExpression(name)
        .map(
            expr -> {
              final int position = getPosition(expr);

              return position >= 0 && position < list.size()
                  ? list.get(position)
                  : Expressions.parse(expr)
                      .flatMap(
                          exp ->
                              list.stream()
                                  .filter(
                                      map ->
                                          Boolean.TRUE.equals(
                                              exp.evaluate(
                                                  identifier ->
                                                      ofNullable((T) map.get(identifier))
                                                          .map(evaluator)
                                                          .orElse(null))))
                                  .findFirst())
                      .orElseGet(HashMap::new);
            })
        .orElseGet(HashMap::new);
  }

  /**
   * Returns the last segment of the path.
   *
   * @param path the given path.
   * @param delimiter the string that separates the segments.
   * @return The optional segment.
   */
  public static Optional<String> getLastSegment(final String path, final String delimiter) {
    return Optional.of(path.lastIndexOf(delimiter))
        .map(i -> i != -1 ? path.substring(i + delimiter.length()) : path)
        .filter(s -> s.length() > 0);
  }

  /**
   * Returns the parent path of <code>path</code>.
   *
   * @param path the given path.
   * @param delimiter the string that separates the segments.
   * @return The parent path.
   */
  public static String getParent(final String path, final String delimiter) {
    return Optional.of(
            path.lastIndexOf(
                delimiter,
                path.endsWith(delimiter)
                    ? (path.length() - delimiter.length() - 1)
                    : path.length()))
        .filter(i -> i != -1)
        .map(i -> i == 0 ? delimiter : path.substring(0, i))
        .orElseGet(() -> path.equals(delimiter) ? delimiter : "");
  }

  private static int getPosition(final String expr) {
    return isInteger(expr) ? Integer.parseInt(expr) : -1;
  }

  /**
   * Returns the segments, but without the empty strings that can be generated by leading, trailing
   * or consecutive delimiters.
   *
   * @param path the path that is split in segments.
   * @param delimiter the string that separates the segments.
   * @return The segment stream.
   */
  public static Stream<String> getSegments(final String path, final String delimiter) {
    return toNonEmptyStrings(segments(path, delimiter));
  }

  /**
   * Returns the segments, but without the empty strings that can be generated by leading, trailing
   * or consecutive delimiters.
   *
   * @param path the path that is split in segments.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment stream.
   */
  public static Stream<String> getSegments(final String path, final Pattern delimiter) {
    return toNonEmptyStrings(segments(path, delimiter));
  }

  public static String getStackTrace(final Throwable e) {
    final StringWriter writer = new StringWriter();

    e.printStackTrace(new PrintWriter(writer));

    return writer.toString();
  }

  private static <T> Optional<T> handleException(
      final Exception e, final Function<Exception, T> error) {
    return error != null
        ? ofNullable(error.apply(e))
        : SideEffect.<Optional<T>>run(() -> printStackTrace(e)).andThenGet(Optional::empty);
  }

  private static void handleException(final Exception e, final Consumer<Exception> error) {
    if (error != null) {
      error.accept(e);
    } else {
      printStackTrace(e);
    }
  }

  /**
   * Loads the logging configuration from the <code>/loggiong.properties</code> resource.
   *
   * @since 2.3.1
   */
  public static void initLogging() {
    initLogging(Util.class.getResourceAsStream("/logging.properties"));
  }

  /**
   * Loads the logging configuration when the system properties <code>java.util.logging
   * .config.class</code> and <code>java.util.logging.config.file</code> are not set.
   *
   * @param configuration the given configuration.
   * @since 2.3.1
   */
  public static void initLogging(final InputStream configuration) {
    if (getProperty("java.util.logging.config.class") == null
        && getProperty("java.util.logging.config.file") == null) {
      tryToDoRethrow(() -> getLogManager().readConfiguration(configuration));
    }
  }

  public static boolean isDate(final String s) {
    return tryToGetSilent(() -> LocalDate.parse(s)).isPresent();
  }

  public static boolean isDouble(final String s) {
    return tryToGetSilent(() -> Double.parseDouble(s)).isPresent();
  }

  public static boolean isEmail(String s) {
    return EMAIL.matcher(s).matches();
  }

  public static boolean isFloat(final String s) {
    return tryToGetSilent(() -> Float.parseFloat(s)).isPresent();
  }

  public static boolean isInstant(final String s) {
    return INSTANT.matcher(!s.endsWith("Z") && !s.endsWith("+00:00") ? (s + "Z") : s).matches();
  }

  public static boolean isInteger(final String s) {
    return tryToGetSilent(() -> Integer.parseInt(s)).isPresent();
  }

  public static boolean isLong(final String s) {
    return tryToGetSilent(() -> Long.parseLong(s)).isPresent();
  }

  public static boolean isUUID(final String s) {
    return tryToGetSilent(() -> fromString(s)).isPresent();
  }

  public static boolean isUri(final String s) {
    return tryToGetSilent(() -> new URI(s)).map(URI::isAbsolute).orElse(false);
  }

  public static Map<String, String> loadProperties(final Supplier<InputStream> in) {
    return tryToGet(() -> readLineConfig(in.get()))
        .orElse(Stream.empty())
        .map(line -> line.split("="))
        .filter(line -> line.length == 2)
        .collect(toMap(line -> line[0], line -> line[1]));
  }

  public static <T> Iterator<T> matcherIterator(
      final Matcher matcher, final Function<Matcher, T> generator) {
    return new Iterator<T>() {
      private boolean found;

      @Override
      public boolean hasNext() {
        found = matcher.find();

        return found;
      }

      @Override
      public T next() {
        if (!found) {
          throw new NoSuchElementException();
        }

        return generator.apply(matcher);
      }
    };
  }

  /**
   * Throws an unchecked exception if <code>o</code> is false.
   *
   * @param o the object to test and return.
   * @since 1.9.2
   */
  public static void must(final boolean o) {
    if (!o) {
      throw new PredicateException("Unmet predicate");
    }
  }

  public static <T> T must(final T o, final Predicate<T> predicate) {
    return must(o, predicate, null);
  }

  /**
   * Throws an unchecked exception if the <code>predicate</code> is not met and returns <code>o
   * </code> otherwise.
   *
   * @param o the object to test and return.
   * @param predicate the predicate to be met.
   * @param report the function that is called to report the failed predicate.
   * @param <T> the type of the object.
   * @return Returns <code>o</code>.
   */
  public static <T> T must(
      final T o, final Predicate<T> predicate, final ConsumerWithException<T> report) {
    if (!predicate.test(o)) {
      if (report != null) {
        tryToDoRethrow(() -> report.accept(o));
      }

      throw new PredicateException("Unmet predicate");
    }

    return o;
  }

  /** Does nothing. */
  public static void nop() {
    // To use as empty runnable.
  }

  /**
   * Does nothing.
   *
   * @param arg the ignored argument.
   * @param <T> the type of the ignored argument.
   */
  public static <T> void nop(final T arg) {
    // To use as empty consumer.
  }

  public static String padWith(final String s, final char c, final int size) {
    return new StringBuilder(s).insert(0, repeat(c, max(0, size - s.length()))).toString();
  }

  public static <T> Optional<T> pathSearch(final Map<String, ? extends T> map, final String path) {
    return pathSearch(map, path, null);
  }

  /**
   * The <code>path</code> is a dot-separated string.
   *
   * @param map the map that is searched.
   * @param path the path that is used for the search.
   * @param evaluator used in the evaluation of expressions.
   * @param <T> the value type.
   * @return The optional value.
   */
  public static <T> Optional<T> pathSearch(
      final Map<String, ? extends T> map, final String path, final Function<T, ?> evaluator) {
    return pathSearch(map, path.split("\\."), evaluator);
  }

  public static <T> Optional<T> pathSearch(
      final Map<String, ? extends T> map, final String[] path) {
    return pathSearch(map, path, null);
  }

  public static <T> Optional<T> pathSearch(
      final Map<String, ? extends T> map, final String[] path, final Function<T, ?> evaluator) {
    return pathSearch(map, Arrays.asList(path), evaluator);
  }

  public static <T> Optional<T> pathSearch(
      final Map<String, ? extends T> map, final List<String> path) {
    return pathSearch(map, path, null);
  }

  /**
   * The <code>map</code> is searched using the <code>path</code>. For segments in the path which
   * represent an array an bracket enclosed expression may follow the name. In that case the array
   * is assumed to contain maps. An optional <code>evaluator</code> can convert values which are
   * found in those maps. The expressions should evaluate either to a boolean or a zero-based
   * offset. The arithmatic operators "+", "-", "*" and "/" may be used to combine fields and
   * literals. The generic type <code>T</code> is the type for the leafs of the tree.
   *
   * @param map the map that is searched.
   * @param path the path that is used for the search.
   * @param evaluator used in the evaluation of expressions.
   * @param <T> the value type.
   * @return The optional value.
   */
  public static <T> Optional<T> pathSearch(
      final Map<String, ? extends T> map, final List<String> path, final Function<T, ?> evaluator) {
    final Function<T, ?> eval = evaluator != null ? evaluator : (a -> a);
    final UnaryOperator<Pair<Object, Integer>> evaluateIfList =
        p ->
            p.first instanceof List
                ? pair(
                    Util.<T>getFromList(
                        (List<Map<String, Object>>) p.first, path.get(p.second), eval),
                    p.second)
                : null;
    final UnaryOperator<Pair<Object, Integer>> evaluateIfMapOr =
        p ->
            p.first instanceof Map
                ? pair(
                    ((Map<String, Object>) p.first).get(stripCondition(path.get(p.second + 1))),
                    p.second + 1)
                : evaluateIfList.apply(p);
    final UnaryOperator<Pair<Object, Integer>> evaluateIfLastOr =
        p ->
            p.second == path.size() - 1
                ? pair(p.first, Integer.MAX_VALUE)
                : evaluateIfMapOr.apply(p);

    return path.isEmpty()
        ? Optional.empty()
        : Stream.iterate(
                Optional.of(new Pair<Object, Integer>(map.get(stripCondition(path.get(0))), 0)),
                pair -> pair.map(evaluateIfLastOr))
            .filter(
                pair ->
                    pair.isEmpty() || pair.map(p -> p.second == Integer.MAX_VALUE).orElse(false))
            .map(pair -> pair.map(p -> (T) p.first))
            .findFirst()
            .orElse(Optional.empty());
  }

  public static void printStackTrace(final Throwable e) {
    getLogger("net.pincette.util.Util").severe(() -> getStackTrace(e));
  }

  /** Can be used for debugging. */
  public static void printStackTrace() {
    try {
      throw new GeneralException("Debug stack trace");
    } catch (final Exception e) {
      printStackTrace(e);
    }
  }

  public static Stream<String> readLineConfig(final InputStream in) throws IOException {
    return readLineConfig(
        new BufferedReader(new EscapedUnicodeFilterReader(new InputStreamReader(in, UTF_8))));
  }

  public static Stream<String> readLineConfig(final BufferedReader in) {
    return readLineConfig(in.lines());
  }

  public static Stream<String> readLineConfig(final Path path) throws IOException {
    return readLineConfig(Files.lines(path, UTF_8));
  }

  /**
   * Returns lines from a stream of strings. Comments start with the "#" character and are removed
   * from the result. Lines can be split over multiple lines with the "\" character. The pieces will
   * be assembled into a single line.
   *
   * @param lines the given stream of lines.
   * @return The generated lines.
   */
  public static Stream<String> readLineConfig(final Stream<String> lines) {
    final List<String> buffer = new ArrayList<>();

    return lines
        .map(line -> (line.indexOf('#') != -1 ? line.substring(0, line.indexOf('#')) : line).trim())
        .filter(line -> line.length() > 0)
        .map(
            line ->
                line.charAt(line.length() - 1) == '\\'
                    ? SideEffect.<String>run(() -> buffer.add(line.substring(0, line.length() - 1)))
                        .andThenGet(() -> null)
                    : (flushLines(buffer) + line))
        .filter(Objects::nonNull);
  }

  private static Pair<String, String> reduceToPair(final Stream<String> s) {
    return s.reduce(
        pair(null, null),
        (pair, segment) ->
            pair.first == null
                ? pair(segment, null)
                : ofNullable(pair.second).map(v -> pair).orElseGet(() -> pair(pair.first, segment)),
        (p1, p2) -> p1);
  }

  public static char[] repeat(final char c, final int count) {
    final char[] result = new char[count];

    fill(result, c);

    return result;
  }

  /**
   * Replaces all matches of <code>pattern</code> in <code>s</code> with the strings generated by
   * <code>replacer</code>.
   *
   * @param s the input string.
   * @param pattern the pattern to match.
   * @param replacer the replacement generator.
   * @return The transformed string.
   */
  public static String replaceAll(
      final String s, final Pattern pattern, final Function<Matcher, String> replacer) {
    final StringBuilder builder = new StringBuilder();
    final Matcher matcher = pattern.matcher(s);
    int position = 0;

    while (matcher.find()) {
      builder.append(s, position, matcher.start());
      builder.append(replacer.apply(matcher));
      position = matcher.end();
    }

    builder.append(s.substring(position));

    return builder.toString();
  }

  /**
   * Replaces all occurrences of strings delimited by "${" and "}".
   *
   * @param s the string that is to transformed.
   * @param parameters the parameters used for the replacement.
   * @return The transformed string.
   */
  public static String replaceParameters(final String s, final Map<String, String> parameters) {
    return replaceParameters(s, parameters, new HashSet<>());
  }

  /**
   * Replaces all occurrences of strings delimited by "${" and "}". When the name in such a string
   * is in <code>leave</code> no replacement is done.
   *
   * @param s the string that is to transformed.
   * @param parameters the parameters used for the replacement.
   * @param leave the names that are excluded from replacement.
   * @return The transformed string.
   */
  public static String replaceParameters(
      final String s, final Map<String, String> parameters, final Set<String> leave) {
    return replaceParameters(s, parameters, '{', '}', leave);
  }

  /**
   * Replaces all occurrences of strings delimited by $<code>leftBrace</code> and <code>rightBrace
   * </code>.
   *
   * @param s the string that is to transformed.
   * @param parameters the parameters used for the replacement.
   * @param leftBrace the left delimiter.
   * @param rightBrace the right delimiter.
   * @return The transformed string.
   */
  public static String replaceParameters(
      final String s,
      final Map<String, String> parameters,
      final char leftBrace,
      final char rightBrace) {
    return replaceParameters(s, parameters, leftBrace, rightBrace, new HashSet<>());
  }

  /**
   * Replaces all occurrences of strings delimited by $<code>leftBrace</code> and <code>rightBrace
   * </code>. When the name in such a string is in <code>leave</code> no replacement is done. When
   * the name contains a colon then the actual name precedes that colon. The string after the colon
   * is the default value for the replacement.
   *
   * @param s the string that is to transformed.
   * @param parameters the parameters used for the replacement.
   * @param leftBrace the left delimiter.
   * @param rightBrace the right delimiter.
   * @param leave the names that are excluded from replacement.
   * @return The transformed string.
   */
  public static String replaceParameters(
      final String s,
      final Map<String, String> parameters,
      final char leftBrace,
      final char rightBrace,
      final Set<String> leave) {
    final Function<Matcher, String> tryDefault =
        matcher -> matcher.groupCount() == 3 ? matcher.group(3) : "";

    return replaceAll(
        s,
        compile(
            "\\$"
                + (leftBrace == '{' || leftBrace == '(' ? "\\" : "")
                + leftBrace
                + "([a-zA-Z0-9_\\-]+)(:([a-zA-Z0-9_\\-]+))?"
                + rightBrace),
        matcher ->
            leave.contains(matcher.group(1))
                ? s.substring(matcher.start(), matcher.end())
                : ofNullable(parameters.get(matcher.group(1)))
                    .orElseGet(() -> tryDefault.apply(matcher)));
  }

  public static Optional<File> resolveFile(final File baseDirectory, final String path) {
    final File file = new File(path);

    return !file.isAbsolute()
        ? tryToGetRethrow(baseDirectory::getCanonicalFile)
            .map(b -> new File(b, path))
            .flatMap(f -> tryToGetRethrow(f::getCanonicalFile))
        : Optional.of(file);
  }

  public static void rethrow(final Throwable t) {
    if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    } else {
      throw new GeneralException(t);
    }
  }

  /**
   * Iterates over the segments in <code>s</code> that are separated by the regular expression
   * <code>delimiter</code>. The segment strings may be empty.
   *
   * @param s the given string.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment iterator.
   * @since 2.0.4
   */
  public static Iterator<CharSequence> segmentIterator(final String s, final Pattern delimiter) {
    final Matcher matcher = delimiter.matcher(shadow(s));

    return new Iterator<>() {
      int lastEnd = -1;
      CharSequence segment;

      @Override
      public boolean hasNext() {
        if (matcher.find()) {
          segment = shadow(s, lastEnd == -1 ? 0 : lastEnd, matcher.start());
          lastEnd = matcher.end();
        } else if (lastEnd < s.length()) {
          segment = lastEnd == -1 ? s : shadow(s, lastEnd, s.length());
          lastEnd = s.length();
        } else {
          segment = null;
        }

        return segment != null;
      }

      @Override
      public CharSequence next() {
        if (segment == null) {
          throw new NoSuchElementException();
        }

        return segment;
      }
    };
  }

  /**
   * Iterates over the segments in <code>s</code> that are separated by the literal string <code>
   * delimiter</code>. The segment strings may be empty.
   *
   * @param s the given string.
   * @param delimiter the literal string that separates the segments.
   * @return The segment iterator.
   * @since 2.0.4
   */
  public static Iterator<CharSequence> segmentIterator(final String s, final String delimiter) {
    return new Iterator<>() {
      int lastEnd = -1;
      CharSequence segment;

      @Override
      public boolean hasNext() {
        final int index = s.indexOf(delimiter, lastEnd == -1 ? 0 : lastEnd);

        if (index != -1) {
          segment = shadow(s, lastEnd == -1 ? 0 : lastEnd, index);
          lastEnd = index + delimiter.length();
        } else if (lastEnd < s.length()) {
          segment = lastEnd == -1 ? s : shadow(s, lastEnd, s.length());
          lastEnd = s.length();
        } else {
          segment = null;
        }

        return segment != null;
      }

      @Override
      public CharSequence next() {
        if (segment == null) {
          throw new NoSuchElementException();
        }

        return segment;
      }
    };
  }

  /**
   * Returns a stream of the segments in <code>s</code> that are separated by the literal string
   * <code>delimiter</code>. The segment strings may be empty.
   *
   * @param s the given string.
   * @param delimiter the literal string that separates the segments.
   * @return The segment stream.
   * @since 2.0.4
   */
  public static Stream<CharSequence> segments(final String s, final String delimiter) {
    return stream(segmentIterator(s, delimiter));
  }

  /**
   * Returns a stream of the segments in <code>s</code> that are separated by the regular expression
   * <code>delimiter</code>. The segment strings may be empty.
   *
   * @param s the given string.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment stream.
   * @since 2.0.4
   */
  public static Stream<CharSequence> segments(final String s, final Pattern delimiter) {
    return stream(segmentIterator(s, delimiter));
  }

  /**
   * Returns a new properties object with the new value.
   *
   * @param properties the given properties.
   * @param key the key.
   * @param value the value.
   * @return The new properties object.
   * @since 1.8
   */
  public static Properties set(final Properties properties, final String key, final String value) {
    final Properties copy = copy(properties);

    copy.setProperty(key, value);

    return copy;
  }

  /**
   * Returns an array of the segments in <code>s</code> that are separated by the literal string
   * <code>delimiter</code>. Empty strings are left out.
   *
   * @param s the given string.
   * @param delimiter the literal string that separates the segments.
   * @return The segment array.
   * @since 2.1.1
   */
  public static String[] split(final String s, final String delimiter) {
    return getSegments(s, delimiter).toArray(String[]::new);
  }

  /**
   * Returns an array of the segments in <code>s</code> that are separated by the regular expression
   * <code>delimiter</code>. Empty strings are left out.
   *
   * @param s the given string.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment array.
   * @since 2.1.1
   */
  public static String[] split(final String s, final Pattern delimiter) {
    return getSegments(s, delimiter).toArray(String[]::new);
  }

  /**
   * Returns a pair of strings in <code>s</code> that are separated by the literal string <code>
   * delimiter</code>. Empty strings are left out. If there is no delimiter the second value of the
   * pair will be <code>null</code>.
   *
   * @param s the given string.
   * @param delimiter the literal string that separates the segments.
   * @return The pair.
   * @since 2.1.1
   */
  public static Pair<String, String> splitPair(final String s, final String delimiter) {
    return reduceToPair(getSegments(s, delimiter));
  }

  /**
   * Returns a pair of strings in <code>s</code> that are separated by the regular expression <code>
   * delimiter</code>. Empty strings are left out. If there is no delimiter the second value of the
   * pair will be <code>null</code>.
   *
   * @param s the given string.
   * @param delimiter the literal string that separates the segments.
   * @return The pair.
   * @since 2.1.1
   */
  public static Pair<String, String> splitPair(final String s, final Pattern delimiter) {
    return reduceToPair(getSegments(s, delimiter));
  }

  private static String stripCondition(final String field) {
    return Optional.of(field.indexOf('['))
        .filter(i -> i != -1)
        .map(i -> field.substring(0, i))
        .orElse(field);
  }

  /**
   * Allows to write things like <code>to(value).apply(v -&gt; ...)</code>. This way you don't need
   * to declare a variable for the value.
   *
   * @param value the given value.
   * @param <T> the type of the given value.
   * @param <R> the type of the returned value.
   * @return The function that accepts a function to apply to the value.
   */
  public static <T, R> Function<FunctionWithException<T, R>, R> to(final T value) {
    return fn -> tryToGetRethrow(() -> fn.apply(value)).orElse(null);
  }

  public static char[] toHex(final byte[] bytes) {
    final char[] result = new char[bytes.length * 2];

    for (int i = 0; i < bytes.length; ++i) {
      final int v = bytes[i] & 0xFF;

      result[i * 2] = HEX_ARRAY[v >>> 4];
      result[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
    }

    return result;
  }

  private static Stream<String> toNonEmptyStrings(final Stream<CharSequence> stream) {
    return stream.filter(cs -> cs.length() > 0).map(CharSequence::toString);
  }

  public static boolean tryToDo(final RunnableWithException run) {
    return tryToDo(run, null);
  }

  public static boolean tryToDo(final RunnableWithException run, final Consumer<Exception> error) {
    try {
      run.run();

      return true;
    } catch (Exception e) {
      handleException(e, error);

      return false;
    }
  }

  public static CompletionStage<Void> tryToDoForever(
      final RunnableWithException run, final Duration retryInterval) {
    return tryToDoForever(run, retryInterval, null);
  }

  /**
   * Repeats the given function until it no longer throws an exception.
   *
   * @param run the given function.
   * @param retryInterval the time between retries.
   * @param onException an optional exception handler.
   * @return The completion stage that completes when the given function succeeds.
   * @since 2.0.1
   */
  public static CompletionStage<Void> tryToDoForever(
      final RunnableWithException run,
      final Duration retryInterval,
      final Consumer<Exception> onException) {
    return tryToGetForever(
            () ->
                SideEffect.<CompletionStage<Object>>run(() -> tryToDoRethrow(run))
                    .andThenGet(() -> completedFuture(VOID)),
            retryInterval,
            onException)
        .thenApply(r -> null);
  }

  public static CompletionStage<Void> tryToDoForever(
      final SupplierWithException<CompletionStage<Void>> run, final Duration retryInterval) {
    return tryToDoForever(run, retryInterval, null);
  }

  /**
   * Repeats the given function until it no longer throws an exception.
   *
   * @param run the given function.
   * @param retryInterval the time between retries.
   * @param onException an optional exception handler.
   * @return The completion stage that completes when the given function succeeds.
   * @since 2.0.1
   */
  public static CompletionStage<Void> tryToDoForever(
      final SupplierWithException<CompletionStage<Void>> run,
      final Duration retryInterval,
      final Consumer<Exception> onException) {
    return tryToGetForever(run, retryInterval, onException);
  }

  public static void tryToDoRethrow(final RunnableWithException run) {
    tryToDo(run, Util::rethrow);
  }

  public static boolean tryToDoSilent(final RunnableWithException run) {
    return tryToDo(run, Util::nop);
  }

  public static <T> boolean tryToDoWith(
      final SupplierWithException<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, null);
  }

  public static <T> boolean tryToDoWith(
      final AutoCloseWrapper<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, null);
  }

  public static <T> boolean tryToDoWith(
      final SupplierWithException<T> resource,
      final ConsumerWithException<T> fn,
      final Consumer<Exception> error) {
    try (final AutoCloseable r = (AutoCloseable) resource.get()) {
      fn.accept((T) r);

      return true;
    } catch (Exception e) {
      handleException(e, error);

      return false;
    }
  }

  public static <T> boolean tryToDoWith(
      final AutoCloseWrapper<T> resource,
      final ConsumerWithException<T> fn,
      final Consumer<Exception> error) {
    try (final AutoCloseWrapper<T> r = resource) {
      fn.accept(r.get());

      return true;
    } catch (Exception e) {
      handleException(e, error);

      return false;
    }
  }

  public static <T> boolean tryToDoWithRethrow(
      final SupplierWithException<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, Util::rethrow);
  }

  public static <T> boolean tryToDoWithRethrow(
      final AutoCloseWrapper<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, Util::rethrow);
  }

  public static <T> boolean tryToDoWithSilent(
      final SupplierWithException<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, Util::nop);
  }

  public static <T> boolean tryToDoWithSilent(
      final AutoCloseWrapper<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, Util::nop);
  }

  public static <T> Optional<T> tryToGet(final SupplierWithException<T> run) {
    return tryToGet(run, null);
  }

  public static <T> Optional<T> tryToGet(
      final SupplierWithException<T> run, final Function<Exception, T> error) {
    try {
      return ofNullable(run.get());
    } catch (Exception e) {
      return handleException(e, error);
    }
  }

  public static <T> CompletionStage<T> tryToGetForever(
      final SupplierWithException<CompletionStage<T>> run, final Duration retryInterval) {
    return tryToGetForever(run, retryInterval, null);
  }

  /**
   * When the supplied completion stage completes with <code>null</code> it is also considered to be
   * a failure and a retry will be performed.
   *
   * @param run the supplier of the completion stage.
   * @param retryInterval the time between retries.
   * @param onException an optional exception handler.
   * @param <T> the object type.
   * @return The completion stage.
   */
  public static <T> CompletionStage<T> tryToGetForever(
      final SupplierWithException<CompletionStage<T>> run,
      final Duration retryInterval,
      final Consumer<Exception> onException) {
    final CompletableFuture<T> future = new CompletableFuture<>();

    tryToGetForever(
        future, run, retryInterval, e -> ofNullable(onException).ifPresent(on -> on.accept(e)));

    return future;
  }

  private static <T> void tryToGetForever(
      final CompletableFuture<T> future,
      final SupplierWithException<CompletionStage<T>> run,
      final Duration retryInterval,
      final Consumer<Exception> onException) {
    final Consumer<Throwable> again =
        e -> {
          if (e instanceof Exception) {
            onException.accept((Exception) e);
          }

          runAsyncAfter(
              () -> tryToGetForever(future, run, retryInterval, onException), retryInterval);
        };

    tryToDo(
        () ->
            run.get()
                .thenAccept(future::complete)
                .exceptionally(
                    e -> {
                      again.accept(e);

                      return null;
                    }),
        again::accept);
  }

  public static <T> Optional<T> tryToGetRethrow(final SupplierWithException<T> run) {
    return tryToGet(
        run,
        e -> {
          throw new GeneralException(e);
        });
  }

  public static <T> Optional<T> tryToGetSilent(final SupplierWithException<T> run) {
    return tryToGet(run, e -> null);
  }

  public static <T, R> Optional<R> tryToGetWith(
      final SupplierWithException<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(resource, fn, null);
  }

  public static <T, R> Optional<R> tryToGetWith(
      final AutoCloseWrapper<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(resource, fn, null);
  }

  /**
   * Tries to calculate a result with a resource that should implement {@link
   * java.lang.AutoCloseable}.
   *
   * @param resource the function that produces the resource.
   * @param fn the function that calculates the result with the resource. It may return <code>null
   *     </code>.
   * @param error the exception handler.
   * @param <T> the resource type.
   * @param <R> the result type.
   * @return The optional value.
   */
  public static <T, R> Optional<R> tryToGetWith(
      final SupplierWithException<T> resource,
      final FunctionWithException<T, R> fn,
      final Function<Exception, R> error) {
    try (final AutoCloseable r = (AutoCloseable) resource.get()) {
      return ofNullable(fn.apply((T) r));
    } catch (Exception e) {
      return handleException(e, error);
    }
  }

  public static <T, R> Optional<R> tryToGetWith(
      final AutoCloseWrapper<T> resource,
      final FunctionWithException<T, R> fn,
      final Function<Exception, R> error) {
    try (final AutoCloseWrapper<T> r = resource) {
      return ofNullable(fn.apply(r.get()));
    } catch (Exception e) {
      return handleException(e, error);
    }
  }

  public static <T, R> Optional<R> tryToGetWithRethrow(
      final SupplierWithException<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(
        resource,
        fn,
        e -> {
          throw new GeneralException(e);
        });
  }

  public static <T, R> Optional<R> tryToGetWithRethrow(
      final AutoCloseWrapper<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(
        resource,
        fn,
        e -> {
          throw new GeneralException(e);
        });
  }

  public static <T, R> Optional<R> tryToGetWithSilent(
      final SupplierWithException<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(resource, fn, e -> null);
  }

  public static <T, R> Optional<R> tryToGetWithSilent(
      final AutoCloseWrapper<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(resource, fn, e -> null);
  }

  public static <T> CompletionStage<T> waitFor(
      final Supplier<CompletionStage<Optional<T>>> condition, final Duration interval) {
    return waitFor(condition, () -> true, interval, ofMillis(MAX_VALUE))
        .thenApply(result -> result.orElse(null));
  }

  public static <T> CompletionStage<Optional<T>> waitFor(
      final Supplier<CompletionStage<Optional<T>>> condition,
      final BooleanSupplier progress,
      final Duration interval,
      final Duration progressTimeout) {
    return waitFor(condition, progress, interval, progressTimeout, new State<>(now()));
  }

  private static <T> CompletionStage<Optional<T>> waitFor(
      final Supplier<CompletionStage<Optional<T>>> condition,
      final BooleanSupplier progress,
      final Duration interval,
      final Duration progressTimeout,
      final State<Instant> lastProgress) {
    return condition
        .get()
        .thenComposeAsync(
            result ->
                result
                    .map(r -> completedFuture(Optional.of(r)))
                    .orElseGet(
                        () -> {
                          if (progress.getAsBoolean()) {
                            lastProgress.set(now());
                          }

                          return lastProgress.get().plus(progressTimeout).isBefore(now())
                              ? completedFuture(empty())
                              : composeAsyncAfter(
                                      () ->
                                          waitFor(
                                              condition,
                                              progress,
                                              interval,
                                              progressTimeout,
                                              lastProgress),
                                      interval)
                                  .toCompletableFuture();
                        }));
  }

  public static Supplier<CompletionStage<Optional<Boolean>>> waitForCondition(
      final Supplier<CompletionStage<Boolean>> condition) {
    return () -> condition.get().thenApply(result -> ofNullable(TRUE.equals(result) ? TRUE : null));
  }

  /**
   * This is for type inference, where <code>T</code> is a complex type, which you would have to
   * declare in multiple places. If only we had typedefs ...
   *
   * @param value the value generator.
   * @param fn the function that should be applied to the value.
   * @return The result of the given function <code>fn</code>
   * @param <T> the value type.
   * @param <R> the result type.
   * @since 2.3
   */
  public static <T, R> R with(final Supplier<T> value, final Function<T, R> fn) {
    return fn.apply(value.get());
  }

  public static class AutoCloseWrapper<T> implements AutoCloseable {
    private final ConsumerWithException<T> close;
    private final SupplierWithException<T> resource;
    private T res;

    private AutoCloseWrapper(
        final SupplierWithException<T> resource, final ConsumerWithException<T> close) {
      this.resource = resource;
      this.close = close;
    }

    public void close() throws Exception {
      if (close != null) {
        close.accept(res);
      }
    }

    private T get() {
      if (res == null) {
        res = tryToGetRethrow(resource).orElse(null);
      }

      return res;
    }
  }

  public static class GeneralException extends RuntimeException {
    public GeneralException(final String message) {
      super(message);
    }

    public GeneralException(final Throwable cause) {
      super(cause);
    }
  }

  public static class PredicateException extends RuntimeException {
    private PredicateException(final String message) {
      super(message);
    }
  }
}
