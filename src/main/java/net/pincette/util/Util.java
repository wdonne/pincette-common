package net.pincette.util;

import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Pair.pair;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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

public class Util

{

  private static final Pattern EMAIL = Pattern.compile("[^@]+@[^@\\.]+\\.[^@]+");

  private Util() {
  }

  /**
   * Adds an element to a collection that calls <code>close</code> on all
   * elements when its own <code>close</code> method is called.
   *
   * @param autoClose the collection that implements
   * {@link java.lang.AutoCloseable}.
   * @param element the element that is to be added.
   * @param <T> the element type.
   * @return The added element.
   */

  public static <T> T
  add(final AutoCloseCollection<T> autoClose, final T element) {
    autoClose.add(element);

    return element;
  }

  /**
   * Produces a stream with all paths from <code>path</code> up to the root
   * path, which is just the <code>delimiter</code>. A trailing
   * <code>delimiter</code> will be discarded.
   *
   * @param path the path that will be decomposed.
   * @param delimiter the delimiter that separates the path segments.
   * @return The stream with the generated paths.
   */

  public static Stream<String>
  allPaths(final String path, final String delimiter) {
    final String[] segments =
        getSegments(path, delimiter).toArray(String[]::new);

    return
        takeWhile(0, i -> i + 1, i -> i < segments.length).map(
            i ->
                delimiter +
                    Arrays.stream(segments, 0, segments.length - i).collect(joining(delimiter))
        );
  }

  /**
   * Wraps a resource in an {@link java.lang.AutoCloseable}.
   *
   * @param resource the given resource.
   * @param close the function that is called with the resource.
   * @param <T> the resource type.
   * @return The wrapped resource.
   */

  public static <T> AutoCloseable
  autoClose(final T resource, final ConsumerWithException<T> close) {
    return () -> close.accept(resource);
  }

  /**
   * Creates an {@link AutoCloseCollection} where the added elements are
   * supposed to have a <code>close</code> method.
   *
   * @return The new collection.
   */

  public static AutoCloseCollection
  autoClose() {
    return autoClose(null);
  }

  /**
   * Creates an {@link AutoCloseCollection}.
   *
   * @param close the function that is called on each element when the
   * collection is closed.
   * @param <T> the element type.
   * @return The new collection.
   */

  public static <T> AutoCloseCollection<T>
  autoClose(final ConsumerWithException<T> close) {
    return new AutoCloseList<>(close);
  }

  /**
   * Compresses using GZIP.
   *
   * @param b the array to be compressed.
   * @return The compressed array.
   */

  public static byte[]
  compress(final byte[] b) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(() -> copy(new ByteArrayInputStream(b), new GZIPOutputStream(out)));

    return out.toByteArray();
  }

  /**
   * Returns an iterator over pairs where the first element is the result of
   * the given iterator and the second the zero-based position in the list.
   *
   * @param iterator the given iterator.
   * @param <T> the element type.
   * @return The new iterator.
   */

  public static <T> Iterator<Pair<T, Integer>>
  countingIterator(final Iterator<T> iterator) {
    return
        new Iterator<Pair<T, Integer>>() {
          private int count = 0;

          public boolean
          hasNext() {
            return iterator.hasNext();
          }

          public Pair<T, Integer>
          next() {
            return new Pair<>(iterator.next(), count++);
          }
        };
  }

  /**
   * Decompresses using GZIP.
   *
   * @param b the array to be decompressed.
   * @return The decompressed array.
   */

  public static byte[]
  decompress(final byte[] b) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(() -> copy(new GZIPInputStream(new ByteArrayInputStream(b)), out));

    return out.toByteArray();
  }

  /**
   * Returns <code>true</code> if the given object equals one of the given
   * values.
   *
   * @param o the given object.
   * @param values the values to compare with.
   * @return The comparison result.
   */

  public static boolean
  equalsOneOf(final Object o, final Object... values) {
    return Arrays.stream(values).anyMatch(v -> v.equals(o));
  }

  private static String
  flushLines(final List<String> buffer) {
    final String result = buffer.stream().collect(joining(""));

    return SideEffect.<String>run(buffer::clear).andThenGet(() -> result);
  }

  /**
   * Allows to write things like <code>from(value).accept(v -&gt; {...})</code>.
   * This way you don't need to declare a variable for the value.
   *
   * @param value the given value.
   * @param <T> the type of the given value.
   * @return The function that accepts a function to consume the value.
   */

  public static <T> Consumer<ConsumerWithException<T>>
  from(final T value) {
    return fn -> tryToDoRethrow(() -> fn.accept(value));
  }

  private static Optional<String>
  getArrayExpression(final String name) {
    return
        Optional.of(name.indexOf('['))
            .filter(i -> i != -1 && name.charAt(name.length() - 1) == ']')
            .map(i -> name.substring(i + 1, name.length() - 1).trim());
  }

  private static <T> Map<String, Object>
  getFromList(
      final List<Map<String, Object>> list,
      final String name,
      final Function<T, ?> evaluator
  ) {
    return
        getArrayExpression(name).map(
            expr ->
            {
              final int position = getPosition(expr);

              return
                  position >= 0 && position < list.size() ?
                      list.get(position) :
                      Expressions
                          .parse(expr)
                          .flatMap(
                              exp ->
                                  list
                                      .stream()
                                      .filter(
                                          map ->
                                              Boolean.TRUE.equals(
                                                  exp.evaluate(
                                                      identifier ->
                                                          evaluator.apply((T) map.get(identifier))
                                                  )
                                              )
                                      )
                                      .findFirst()
                          )
                          .orElseGet(HashMap::new);
            }
        )
            .orElseGet(HashMap::new);
  }

  /**
   * Returns the last segment of the path.
   *
   * @param path the given path.
   * @param delimiter the regular expression that separates the segments.
   * @return The optional segment.
   */

  public static Optional<String>
  getLastSegment(final String path, final String delimiter) {
    return
        Optional
            .of(getSegments(path, delimiter).collect(toList()))
            .filter(segments -> !segments.isEmpty())
            .map(segments -> segments.get(segments.size() - 1));
  }

  private static int
  getPosition(final String expr) {
    return isInteger(expr) ? Integer.parseInt(expr) : -1;
  }

  /**
   * Returns the segments, but without the empty strings that can be generated
   * by leading, trailing or consecutive delimiters.
   *
   * @param path the path that is split in segments.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment stream.
   */

  public static Stream<String>
  getSegments(final String path, final String delimiter) {
    return Arrays.stream(path.split(delimiter)).filter(seg -> seg.length() > 0);
  }

  public static String
  getStackTrace(final Throwable e) {
    final StringWriter writer = new StringWriter();

    e.printStackTrace(new PrintWriter(writer));

    return writer.toString();
  }

  private static <T> Optional<T>
  handleException(final Exception e, final Function<Exception, T> error) {
    return
        error != null ?
            Optional.ofNullable(error.apply(e)) :
            SideEffect.<Optional<T>>run(() -> printStackTrace(e)).andThenGet(Optional::empty);
  }

  private static void
  handleException(final Exception e, final Consumer<Exception> error) {
    if (error != null) {
      error.accept(e);
    } else {
      printStackTrace(e);
    }
  }

  public static boolean
  isDate(final String s) {
    return tryToGetSilent(() -> LocalDate.parse(s)).isPresent();
  }

  public static boolean
  isDouble(final String s) {
    return tryToGetSilent(() -> Double.parseDouble(s)).isPresent();
  }

  public static boolean
  isEmail(String s) {
    return EMAIL.matcher(s).matches();
  }

  public static boolean
  isFloat(final String s) {
    return tryToGetSilent(() -> Float.parseFloat(s)).isPresent();
  }

  public static boolean
  isInstant(final String s) {
    return tryToGetSilent(() -> Instant.parse(s.endsWith("Z") ? s : (s + "Z"))).isPresent();
  }

  public static boolean
  isInteger(final String s) {
    return tryToGetSilent(() -> Integer.parseInt(s)).isPresent();
  }

  public static boolean
  isLong(final String s) {
    return tryToGetSilent(() -> Long.parseLong(s)).isPresent();
  }

  public static boolean
  isUri(final String s) {
    return tryToGetSilent(() -> new URI(s)).map(URI::isAbsolute).orElse(false);
  }

  public static Map<String, String>
  loadProperties(final Supplier<InputStream> in) {
    return
        tryToGet(() -> readLineConfig(in.get())).orElse(Stream.empty())
            .map(line -> line.split("="))
            .filter(line -> line.length == 2)
            .collect(toMap(line -> line[0], line -> line[1]));
  }

  public static <T> T
  must(final T o, final Predicate<T> predicate) {
    return must(o, predicate, null);
  }

  /**
   * Throws an unchecked exception if the <code>predicate</code> is not met
   * and returns <code>o</code> otherwise.
   *
   * @param o the object to test and return.
   * @param predicate the predicate to be met.
   * @param report the function that is called to report the failed predicate.
   * @param <T> the type of the object.
   * @return Returns <code>o</code>.
   */

  public static <T> T
  must(final T o, final Predicate<T> predicate, final ConsumerWithException<T> report) {
    if (!predicate.test(o)) {
      if (report != null) {
        tryToDoRethrow(() -> report.accept(o));
      }

      throw new PredicateException("Unmet predicate");
    }

    return o;
  }

  /**
   * Does nothing.
   */

  public static void
  nop() {
    // To use as empty runnable.
  }

  /**
   * Does nothing.
   */

  public static <T> void
  nop(final T arg) {
    // To use as empty consumer.
  }

  public static <T> Optional<T>
  pathSearch(final Map<String, ? extends T> map, final String path) {
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

  public static <T> Optional<T>
  pathSearch(
      final Map<String, ? extends T> map,
      final String path,
      final Function<T, ?> evaluator
  ) {
    return pathSearch(map, path.split("\\."), evaluator);
  }

  public static <T> Optional<T>
  pathSearch(final Map<String, ? extends T> map, final String[] path) {
    return pathSearch(map, path, null);
  }

  public static <T> Optional<T>
  pathSearch(
      final Map<String, ? extends T> map,
      final String[] path,
      final Function<T, ?> evaluator
  ) {
    return pathSearch(map, Arrays.asList(path), evaluator);
  }

  public static <T> Optional<T>
  pathSearch(final Map<String, ? extends T> map, final List<String> path) {
    return pathSearch(map, path, null);
  }

  /**
   * The <code>map</code> is searched using the <code>path</code>. For segments
   * in the path which represent an array an bracket enclosed expression may
   * follow the name. In that case the array is assumed to contain maps. An
   * optional <code>evaluator</code> can convert values which are found in
   * those maps. The expressions should evaluate either to a boolean or a
   * zero-based offset. The arithmatic operators "+", "-", "*" and "/" may be
   * used to combine fields and literals. The generic type <code>T</code> is
   * the type for the leafs of the tree.
   *
   * @param map the map that is searched.
   * @param path the path that is used for the search.
   * @param evaluator used in the evaluation of expressions.
   * @param <T> the value type.
   * @return The optional value.
   */

  public static <T> Optional<T>
  pathSearch(
      final Map<String, ? extends T> map,
      final List<String> path,
      final Function<T, ?> evaluator
  ) {
    final Function<T, ?> eval = evaluator != null ? evaluator : (a -> a);
    final UnaryOperator<Pair<Object, Integer>> evaluateIfList =
        p ->
            p.first instanceof List ?
                pair(
                    Util.<T>getFromList(
                        (List<Map<String, Object>>) p.first,
                        path.get(p.second),
                        eval
                    ),
                    p.second
                ) :
                null;
    final UnaryOperator<Pair<Object, Integer>> evaluateIfMapOr =
        p ->
            p.first instanceof Map ?
                pair(
                    ((Map<String, Object>) p.first).get(stripCondition(path.get(p.second + 1))),
                    p.second + 1
                ) :
                evaluateIfList.apply(p);
    final UnaryOperator<Pair<Object, Integer>> evaluateIfLastOr =
        p ->
            p.second == path.size() - 1 ?
                pair(p.first, Integer.MAX_VALUE) : evaluateIfMapOr.apply(p);

    return
        path.isEmpty() ?
            Optional.empty() :
            Stream.iterate(
                Optional.of(new Pair<Object, Integer>(map.get(stripCondition(path.get(0))), 0)),
                pair -> pair.map(evaluateIfLastOr)
            )
                .filter(
                    pair ->
                        !pair.isPresent() ||
                            pair.map(p -> p.second == Integer.MAX_VALUE).orElse(false)
                )
                .map(pair -> pair.map(p -> (T) p.first))
                .findFirst()
                .orElse(Optional.empty());
  }


  public static void
  printStackTrace(final Throwable e) {
    getLogger("net.pincette.util.Util").severe(getStackTrace(e));
  }

  public static Stream<String>
  readLineConfig(final InputStream in) throws IOException {
    return
        readLineConfig(
            new BufferedReader(new EscapedUnicodeFilterReader(new InputStreamReader(in, "UTF-8")))
        );
  }

  public static Stream<String>
  readLineConfig(final BufferedReader in) {
    return readLineConfig(in.lines());
  }

  public static Stream<String>
  readLineConfig(final Path path) throws IOException {
    return readLineConfig(Files.lines(path, Charset.forName("UTF-8")));
  }

  /**
   * Returns lines from a stream of strings. Comments start with the "#"
   * character and are removed from the result. Lines can be split over
   * multiple lines with the "\" character. The pieces will be assembled into
   * a single line.
   *
   * @param lines the given stream of lines.
   * @return The generated lines.
   */

  public static Stream<String>
  readLineConfig(final Stream<String> lines) {
    final List<String> buffer = new ArrayList<>();

    return
        lines
            .map(
                line ->
                    (
                        line.indexOf('#') != -1 ?
                            line.substring(0, line.indexOf('#')) : line
                    ).trim()
            )
            .filter(line -> line.length() > 0)
            .map(
                line ->
                    line.charAt(line.length() - 1) == '\\' ?
                        SideEffect
                            .<String>run(() -> buffer.add(line.substring(0, line.length() - 1)))
                            .andThenGet(() -> null) :
                        (flushLines(buffer) + line)
            )
            .filter(Objects::nonNull);
  }

  public static void
  rethrow(final Throwable e) {
    throw new GeneralException(e);
  }

  public static <T> Stream<T>
  stream(final Iterator<T> iterator) {
    return
        StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE
            ),
            false
        );
  }

  public static <T> Stream<T>
  stream(final Enumeration<T> enumeration) {
    return
        stream(
            new Iterator<T>() {
              public boolean
              hasNext() {
                return enumeration.hasMoreElements();
              }

              public T
              next() {
                if (!enumeration.hasMoreElements()) {
                  throw new NoSuchElementException();
                }

                return enumeration.nextElement();
              }
            }
        );
  }

  private static String
  stripCondition(final String field) {
    return
        Optional
            .of(field.indexOf('['))
            .filter(i -> i != -1)
            .map(i -> field.substring(0, i))
            .orElse(field);
  }

  /**
   * Iterates sequentially until the predicate returns <code>false</code>.
   *
   * @param seed the initial value.
   * @param f the function that calculates the next value.
   * @param p the predicate.
   * @param <T> the value type.
   * @return The generated stream.
   */

  public static <T> Stream<T>
  takeWhile(final T seed, final UnaryOperator<T> f, final Predicate<T> p) {
    return
        stream(
            new Iterator<T>() {
              private T current = seed;

              public boolean
              hasNext() {
                return p.test(current);
              }

              public T
              next() {
                if (!hasNext()) {
                  throw new NoSuchElementException();
                }

                final T result = current;

                current = f.apply(current);

                return result;
              }
            }
        );
  }

  /**
   * Allows to write things like <code>to(value).apply(v -&gt; ...)</code>.
   * This way you don't need to declare a variable for the value.
   *
   * @param value the given value.
   * @param <T> the type of the given value.
   * @param <R> the type of the returned value.
   * @return The function that accepts a function to apply to the value.
   */

  public static <T, R> Function<FunctionWithException<T, R>, R>
  to(final T value) {
    return fn -> tryToGetRethrow(() -> fn.apply(value)).orElse(null);
  }

  public static boolean
  tryToDo(final RunnableWithException run) {
    return tryToDo(run, null);
  }

  public static boolean
  tryToDo(final RunnableWithException run, final Consumer<Exception> error) {
    try {
      run.run();

      return true;
    } catch (Exception e) {
      handleException(e, error);

      return false;
    }
  }

  public static void
  tryToDoRethrow(final RunnableWithException run) {
    tryToDo(run, Util::rethrow);
  }

  public static boolean
  tryToDoSilent(final RunnableWithException run) {
    return tryToDo(run, Util::nop);
  }

  public static <T> boolean
  tryToDoWith(final SupplierWithException<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, null);
  }

  public static <T> boolean
  tryToDoWith(
      final SupplierWithException<T> resource,
      final ConsumerWithException<T> fn,
      final Consumer<Exception> error
  ) {
    try (final AutoCloseable r = (AutoCloseable) resource.get()) {
      fn.accept((T) r);

      return true;
    } catch (Exception e) {
      handleException(e, error);

      return false;
    }
  }

  public static <T> boolean
  tryToDoWithRethrow(final SupplierWithException<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, Util::rethrow);
  }

  public static <T> boolean
  tryToDoWithSilent(final SupplierWithException<T> resource, final ConsumerWithException<T> fn) {
    return tryToDoWith(resource, fn, Util::nop);
  }


  public static <T> Optional<T>
  tryToGet(final SupplierWithException<T> run) {
    return tryToGet(run, null);
  }


  public static <T> Optional<T>
  tryToGet(final SupplierWithException<T> run, final Function<Exception, T> error) {
    try {
      return Optional.ofNullable(run.get());
    } catch (Exception e) {
      return handleException(e, error);
    }
  }

  public static <T> Optional<T>
  tryToGetRethrow(final SupplierWithException<T> run) {
    return
        tryToGet(
            run,
            e -> {
              throw new GeneralException(e);
            }
        );
  }

  public static <T> Optional<T>
  tryToGetSilent(final SupplierWithException<T> run) {
    return tryToGet(run, e -> null);
  }

  public static <T, R> Optional<R>
  tryToGetWith(final SupplierWithException<T> resource, final FunctionWithException<T, R> fn) {
    return tryToGetWith(resource, fn, null);
  }

  /**
   * Tries to calculate a result with a resource that should implement
   * {@link java.lang.AutoCloseable}.
   *
   * @param resource the function that produces the resource.
   * @param fn the function that calculates the result with the resource. It
   * may return <code>null</code>.
   * @param error the exception handler.
   * @param <T> the resource type.
   * @param <R> the result type.
   * @return The optional value.
   */

  public static <T, R> Optional<R>
  tryToGetWith(
      final SupplierWithException<T> resource,
      final FunctionWithException<T, R> fn,
      final Function<Exception, R> error
  ) {
    try (final AutoCloseable r = (AutoCloseable) resource.get()) {
      return Optional.ofNullable(fn.apply((T) r));
    } catch (Exception e) {
      return handleException(e, error);
    }
  }

  public static <T, R> Optional<R>
  tryToGetWithRethrow(
      final SupplierWithException<T> resource,
      final FunctionWithException<T, R> fn
  ) {
    return
        tryToGetWith(
            resource,
            fn,
            e -> {
              throw new GeneralException(e);
            }
        );
  }

  public static <T, R> Optional<R>
  tryToGetWithSilent(
      final SupplierWithException<T> resource,
      final FunctionWithException<T, R> fn
  ) {
    return tryToGetWith(resource, fn, e -> null);
  }

  private static class AutoCloseList<T> implements AutoCloseCollection<T>

  {

    private final ConsumerWithException<T> close;
    private final List<AutoCloseable> list = new ArrayList<>();

    private AutoCloseList(final ConsumerWithException<T> close) {
      this.close = close;
    }

    public <U> U
    add(final U element, final ConsumerWithException<U> close) {
      list.add(autoClose(element, close));

      return element;
    }

    public T
    add(final T element) {
      if (!(element instanceof AutoCloseable) && close == null) {
        throw new GeneralException("No close function.");
      }

      list.add(
          element instanceof AutoCloseable ? (AutoCloseable) element : autoClose(element, close)
      );

      return element;
    }

    public void
    close() throws Exception {
      list.forEach(e -> tryToDo(e::close));
    }

  } // AutoCloseList

  public static class GeneralException extends RuntimeException

  {

    public GeneralException(final String message) {
      super(message);
    }

    public GeneralException(final Throwable cause) {
      super(cause);
    }

  } // GeneralException

  public static class PredicateException extends RuntimeException

  {

    private PredicateException(final String message) {
      super(message);
    }

  } // PredicateException

} // Util
