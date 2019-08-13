package net.pincette.util;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.ScheduledCompletionStage.composeAsyncAfter;
import static net.pincette.util.StreamUtil.last;
import static net.pincette.util.StreamUtil.takeWhile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
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
import net.pincette.function.ConsumerWithException;
import net.pincette.function.FunctionWithException;
import net.pincette.function.RunnableWithException;
import net.pincette.function.SideEffect;
import net.pincette.function.SupplierWithException;
import net.pincette.io.EscapedUnicodeFilterReader;

/**
 * General purpose utility functions.
 *
 * @author Werner Donn\u00e9
 */
public class Util {
  private static final Pattern EMAIL =
      Pattern.compile(
          "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");
  private static final Pattern INSTANT =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?(Z|\\+00:00)");

  private Util() {}

  /**
   * Produces a stream with all paths from <code>path</code> up to the root path, which is just the
   * <code>delimiter</code>. A trailing <code>delimiter</code> will be discarded.
   *
   * @param path the path that will be decomposed.
   * @param delimiter the delimiter that separates the path segments.
   * @return The stream with the generated paths.
   */
  public static Stream<String> allPaths(final String path, final String delimiter) {
    final String leading = path.startsWith(delimiter) ? delimiter : "";
    final String[] segments = getSegments(path, delimiter).toArray(String[]::new);

    return takeWhile(0, i -> i + 1, i -> i < segments.length)
        .map(
            i ->
                leading
                    + Arrays.stream(segments, 0, segments.length - i).collect(joining(delimiter)));
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
   * Compresses using GZIP.
   *
   * @param b the array to be compressed.
   * @return The compressed array.
   */
  public static byte[] compress(final byte[] b) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(() -> copy(new ByteArrayInputStream(b), new GZIPOutputStream(out)));

    return out.toByteArray();
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

    tryToDoRethrow(() -> copy(new GZIPInputStream(new ByteArrayInputStream(b)), out));

    return out.toByteArray();
  }

  /**
   * Executes <code>runnable</code> infinitely.
   *
   * @param runnable a function that may throw an exception, which will be rethrown.
   */
  public static void doForever(final RunnableWithException runnable) {
    doForever(runnable, Util::rethrow);
  }

  /**
   * Executes <code>runnable</code> infinitely.
   *
   * @param runnable a function that may throw an exception, which will be rethrown.
   * @param error the function that deals with an exception.
   */
  public static void doForever(
      final RunnableWithException runnable, final Consumer<Exception> error) {
    tryToDo(
        () -> {
          while (true) {
            runnable.run();
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
    return Arrays.stream(values).anyMatch(v -> v.equals(o));
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
                                                      Optional.ofNullable((T) map.get(identifier))
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
   * @param delimiter the regular expression that separates the segments.
   * @return The optional segment.
   */
  public static Optional<String> getLastSegment(final String path, final String delimiter) {
    return last(getSegments(path, delimiter));
  }

  /**
   * Returns the parent path of <code>path</code>.
   *
   * @param path the given path.
   * @param delimiter the regular expression that separates the segments.
   * @return The parent path.
   */
  public static String getParent(final String path, final String delimiter) {
    final List<String> segments = getSegments(path, delimiter).collect(toList());

    return (path.startsWith(delimiter) ? delimiter : "")
        + join(delimiter, segments.subList(0, segments.size() - 1));
  }

  private static int getPosition(final String expr) {
    return isInteger(expr) ? Integer.parseInt(expr) : -1;
  }

  /**
   * Returns the segments, but without the empty strings that can be generated by leading, trailing
   * or consecutive delimiters.
   *
   * @param path the path that is split in segments.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment stream.
   */
  public static Stream<String> getSegments(final String path, final String delimiter) {
    return Arrays.stream(
            Optional.of(path.split(delimiter))
                .filter(split -> split.length > 0)
                .orElse(new String[] {path}))
        .filter(seg -> seg.length() > 0);
  }

  public static String getStackTrace(final Throwable e) {
    final StringWriter writer = new StringWriter();

    e.printStackTrace(new PrintWriter(writer));

    return writer.toString();
  }

  private static <T> Optional<T> handleException(
      final Exception e, final Function<Exception, T> error) {
    return error != null
        ? Optional.ofNullable(error.apply(e))
        : SideEffect.<Optional<T>>run(() -> printStackTrace(e)).andThenGet(Optional::empty);
  }

  private static void handleException(final Exception e, final Consumer<Exception> error) {
    if (error != null) {
      error.accept(e);
    } else {
      printStackTrace(e);
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

  /** Does nothing. */
  public static <T> void nop(final T arg) {
    // To use as empty consumer.
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
                    !pair.isPresent() || pair.map(p -> p.second == Integer.MAX_VALUE).orElse(false))
            .map(pair -> pair.map(p -> (T) p.first))
            .findFirst()
            .orElse(Optional.empty());
  }

  public static void printStackTrace(final Throwable e) {
    getLogger("net.pincette.util.Util").severe(getStackTrace(e));
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
   * </code>. When the name in such a string is in <code>leave</code> no replacement is done.
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
    final String left = new String(new char[] {'$', leftBrace});
    int position = 0;
    final StringBuilder result = new StringBuilder(s.length() * 2);

    for (int i = s.indexOf(left); i != -1; i = s.indexOf(left, position)) {
      final int j = s.indexOf(rightBrace, i + 2);

      if (j == -1) // Syntax error.
      {
        return s;
      }

      final int colon = s.indexOf(":-", i + 2);
      String defaultValue;
      String name;

      if (colon != -1 && colon < j) {
        name = s.substring(i + 2, colon);
        defaultValue = s.substring(colon + 2, j);
      } else {
        name = s.substring(i + 2, j);
        defaultValue = "";
      }

      result.append(s, position, i);
      position = j + 1;

      result.append(
          leave.contains(name)
              ? s.substring(i, position)
              : Optional.ofNullable(parameters.get(name)).orElse(defaultValue));
    }

    if (position < s.length()) {
      result.append(s.substring(position));
    }

    return result.toString();
  }

  public static void rethrow(final Throwable e) {
    throw new GeneralException(e);
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
      return Optional.ofNullable(run.get());
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
    final Supplier<CompletionStage<T>> again =
        () ->
            composeAsyncAfter(
                () -> tryToGetForever(run, retryInterval, onException), retryInterval);
    final Consumer<Exception> ex =
        e -> Optional.ofNullable(onException).ifPresent(on -> on.accept(e));

    return tryToGet(
            run, e -> SideEffect.<CompletionStage<T>>run(() -> ex.accept(e)).andThenGet(again))
        .map(
            stage ->
                stage
                    .exceptionally(
                        e ->
                            SideEffect.<T>run(
                                    () -> {
                                      if (e instanceof Exception) {
                                        ex.accept((Exception) e);
                                      }
                                    })
                                .andThenGet(() -> null))
                    .thenComposeAsync(
                        value -> value != null ? completedFuture(value) : again.get()))
        .orElse(null);
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
      return Optional.ofNullable(fn.apply((T) r));
    } catch (Exception e) {
      return handleException(e, error);
    }
  }

  public static <T, R> Optional<R> tryToGetWith(
      final AutoCloseWrapper<T> resource,
      final FunctionWithException<T, R> fn,
      final Function<Exception, R> error) {
    try (final AutoCloseWrapper<T> r = resource) {
      return Optional.ofNullable(fn.apply(r.get()));
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

    private T get() throws Exception {
      if (res == null) {
        res = resource.get();
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
