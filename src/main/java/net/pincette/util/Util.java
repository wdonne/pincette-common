package net.pincette.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
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
import net.pincette.io.StreamConnector;



/**
 * General purpose utility functions.
 * @author Werner Donn\u00e9
 */

public class Util

{

  final private static Pattern EMAIL =
    Pattern.compile("[^@]+@[^@\\.]+\\.[^@]+");



  /**
   * Adds an element to a collection that calls <code>close</code> on all
   * elements when its own <code>close</code> method is called.
   * @param autoClose the collection that implements
   * {@link java.lang.AutoCloseable}.
   * @param element the element that is to be added.
   * @param <T> the element type.
   * @return The added element.
   */

  public static <T> T
  add(final AutoCloseCollection autoClose, final T element)
  {
    autoClose.add(element);

    return element;
  }



  /**
   * Produces a stream with all paths from <code>path</code> up to the root
   * path, which is just the <code>delimiter</code>. A trailing
   * <code>delimiter</code> will be discarded.
   * @param path the path that will be decomposed.
   * @param delimiter the delimiter that separates the path segments.
   * @return The stream with the generated paths.
   */

  public static Stream<String>
  allPaths(final String path, final String delimiter)
  {
    final String[] segments =
      getSegments(path, delimiter).toArray(String[]::new);

    return
      takeWhile(0, i -> i + 1, i -> i < segments.length).
        map
        (
          i ->
          delimiter +
            Arrays.stream(segments, 0, segments.length - i).
              collect(joining(delimiter))
        );
  }



  /**
   * Wraps a resource in an {@link java.lang.AutoCloseable}.
   * @param resource the given resource.
   * @param close the function that is called with the resource.
   * @param <T> the resource type.
   * @return The wrapped resource.
   */

  public static <T> AutoCloseable
  autoClose(final T resource, final ConsumerWithException<T> close)
  {
    return () -> close.accept(resource);
  }



  /**
   * Creates an {@link AutoCloseCollection} where the added elements are
   * supposed to have a <code>close</code> method.
   * @return The new collection.
   */

  public static AutoCloseCollection
  autoClose()
  {
    return autoClose(null);
  }



  /**
   * Creates an {@link AutoCloseCollection}.
   * @param close the function that is called on each element when the
   *              collection is closed.
   * @param <T> the element type.
   * @return The new collection.
   */

  public static <T> AutoCloseCollection<T>
  autoClose(final ConsumerWithException<T> close)
  {
    return new AutoCloseList<T>(close);
  }



  /**
   * Compresses using GZIP.
   * @param b the array to be compressed.
   * @return The compressed array.
   */

  public static byte[]
  compress(final byte[] b)
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow
    (
      () ->
        StreamConnector.
          copy(new ByteArrayInputStream(b), new GZIPOutputStream(out))
    );

    return out.toByteArray();
  }



  /**
   * Returns an iterator over pairs where the first element is the result of
   * the given iterator and the second the zero-based position in the list.
   * @param iterator the given iterator.
   * @param <T> the element type.
   * @return The new iterator.
   */

  public static <T> Iterator<Pair<T,Integer>>
  countingIterator(final Iterator<T> iterator)
  {
    return
      new Iterator<Pair<T, Integer>>()
      {
        private int count = 0;

        public boolean
        hasNext()
        {
          return iterator.hasNext();
        }

        public Pair<T, Integer>
        next()
        {
          return new Pair<>(iterator.next(), count++);
        }
      };
  }



  /**
   * Decompresses using GZIP.
   * @param b the array to be decompressed.
   * @return The decompressed array.
   */

  public static byte[]
  decompress(final byte[] b)
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow
      (
        () ->
          StreamConnector.
            copy(new GZIPInputStream(new ByteArrayInputStream(b)), out)
      );

    return out.toByteArray();
  }



  /**
   * Returns <code>true</code> if the given object equals one of the given
   * values.
   * @param o the given object.
   * @param values the values to compare with.
   * @return The comparison result.
   */

  public static boolean
  equalsOneOf(final Object o, final Object... values)
  {
    return Arrays.stream(values).anyMatch(v -> v.equals(o));
  }



  private static String
  flushLines(final List<String> buffer)
  {
    final String result = buffer.stream().collect(joining(""));

    return
      SideEffect.<String>run(buffer::clear).andThenGet(() -> result);
  }



  private static Optional<String>
  getArrayExpression(final String name)
  {
    final int index = name.indexOf('[');

    return
      index != -1 && name.charAt(name.length() - 1) == ']' ?
        Optional.of(name.substring(index + 1, name.length() - 1).trim()) :
        Optional.empty();
  }



  private static <T> Map<String,Object>
  getFromList
  (
    final List<Map<String,Object>> list,
    final String name,
    final Function<T,?> evaluator
  )
  {
    return
      getArrayExpression(name).
        map
        (
          expr ->
            {
              final int position = getPosition(expr);

              return
                position >= 0 && position < list.size() ?
                  list.get(position) :
                  Expressions.parse(expr).
                    flatMap
                    (
                      exp ->
                        list.
                          stream().
                          filter
                          (
                            map ->
                              Boolean.TRUE.equals
                              (
                                exp.evaluate
                                (
                                  identifier ->
                                    evaluator.apply((T) map.get(identifier))
                                )
                              )
                          ).
                          findFirst()
                    ).
                    orElseGet(HashMap<String,Object>::new);
            }
        ).
        orElseGet(HashMap<String,Object>::new);
  }



  /**
   * Returns the last segment of the path.
   * @param path the given path.
   * @param delimiter the regular expression that separates the segments.
   * @return The optional segment.
   */

  public static Optional<String>
  getLastSegment(final String path, final String delimiter)
  {
    final List<String> segments =
      getSegments(path, delimiter).collect(toList());

    return
      segments.size() > 0 ?
        Optional.of(segments.get(segments.size() - 1)) : Optional.empty();
  }



  private static int
  getPosition(final String expr)
  {
    return isInteger(expr) ? Integer.parseInt(expr) : -1;
  }



  /**
   * Returns the segments, but without the empty strings that can be generated
   * by leading, trailing or consecutive delimiters.
   * @param path the path that is split in segments.
   * @param delimiter the regular expression that separates the segments.
   * @return The segment stream.
   */

  public static Stream<String>
  getSegments(final String path, final String delimiter)
  {
    return Arrays.stream(path.split(delimiter)).filter(seg -> seg.length() > 0);
  }



  private static <T> Optional<T>
  handleException(final Exception e, final Function<Exception,T> error)
  {
    return
      error != null ?
        Optional.ofNullable(error.apply(e)) :
        SideEffect.<Optional<T>>run(e::printStackTrace).
          andThenGet(Optional::empty);
  }



  private static void
  handleException(final Exception e, final Consumer<Exception> error)
  {
    if (error != null)
    {
      error.accept(e);
    }
    else
    {
      e.printStackTrace();
    }
  }



  public static int
  hash(final Object... objects)
  {
    return
      Arrays.stream(objects).
        reduce
        (
          0,
          (r, o) -> 41 * (41 * (41 + (o != null ? o.hashCode() : 0)) + r),
          (r1, r2) -> r1
        );
  }



  public static boolean
  isDate(final String s)
  {
    return tryToGetSilent(() -> LocalDate.parse(s)).isPresent();
  }



  public static boolean
  isDouble(final String s)
  {
    return tryToGetSilent(() -> Double.parseDouble(s)).isPresent();
  }



  public static boolean
  isEmail(String s)
  {
    return EMAIL.matcher(s).matches();
  }



  public static boolean
  isFloat(final String s)
  {
    return tryToGetSilent(() -> Float.parseFloat(s)).isPresent();
  }



  public static boolean
  isInstant(final String s)
  {
    return
      tryToGetSilent(() -> Instant.parse(s.endsWith("Z") ? s : (s + "Z"))).
        isPresent();
  }



  public static boolean
  isInteger(final String s)
  {
    return tryToGetSilent(() -> Integer.parseInt(s)).isPresent();
  }



  public static boolean
  isLong(final String s)
  {
    return tryToGetSilent(() -> Long.parseLong(s)).isPresent();
  }



  public static boolean
  isUri(final String s)
  {
    return tryToGetSilent(() -> new URI(s).isAbsolute()).orElse(false);
  }



  public static Map<String,String>
  loadProperties(final Supplier<InputStream> in)
  {
    return
      tryToGet(() -> readLineConfig(in.get())).orElse(Stream.empty()).
        map(line -> line.split("=")).
        filter(line -> line.length == 2).
        collect(toMap(line -> line[0], line -> line[1]));
  }



  /**
   * Throws an unchecked exception if the <code>predicate</code> is not met
   * and returns <code>o</code> otherwise.
   * @param o the object to test and return.
   * @param predicate the predicate to be met.
   * @param <T> the type of the object.
   * @return Returns <code>o</code>.
   */

  public static <T> T
  must(final T o, final Predicate<T> predicate)
  {
    if (!predicate.test(o))
    {
      throw new RuntimeException("Unmet predicate");
    }

    return o;
  }



  public static <T> Optional<T>
  pathSearch(final Map<String,? extends T> map, final String path)
  {
    return pathSearch(map, path, null);
  }



  /**
   * The <code>path</code> is a dot-separated string.
   * @param map the map that is searched.
   * @param path the path that is used for the search.
   * @param evaluator used in the evaluation of expressions.
   * @param <T> the value type.
   * @return The optional value.
   */

  public static <T> Optional<T>
  pathSearch
  (
    final Map<String,? extends T> map,
    final String path,
    final Function<T,?> evaluator
  )
  {
    return pathSearch(map, path.split("\\."), evaluator);
  }



  public static <T> Optional<T>
  pathSearch(final Map<String,? extends T> map, final String[] path)
  {
    return pathSearch(map, path, null);
  }



  public static <T> Optional<T>
  pathSearch
  (
    final Map<String,? extends T> map,
    final String[] path,
    final Function<T,?> evaluator
  )
  {
    return pathSearch(map, Arrays.asList(path), evaluator);
  }



  public static <T> Optional<T>
  pathSearch(final Map<String,? extends T> map, final List<String> path)
  {
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
   * @param map the map that is searched.
   * @param path the path that is used for the search.
   * @param evaluator used in the evaluation of expressions.
   * @param <T> the value type.
   * @return The optional value.
   */

  public static <T> Optional<T>
  pathSearch
  (
    final Map<String,? extends T> map,
    final List<String> path,
    final Function<T,?> evaluator
  )
  {
    return
      path.size() == 0 ?
        Optional.empty() :
        Stream.iterate
        (
          Optional.of
          (
            new Pair<Object,Integer>(map.get(stripCondition(path.get(0))), 0)
          ),
          pair ->
            pair.map
            (
              p ->
                p.second == path.size() - 1 ?
                  new Pair<>(p.first, Integer.MAX_VALUE) :
                  (
                    p.first instanceof Map ?
                      new Pair<>
                      (
                        ((Map<String,Object>) p.first).get
                        (
                          stripCondition(path.get(p.second + 1))
                        ),
                        p.second + 1
                      ) :
                      (
                        p.first instanceof List ?
                          new Pair<Object,Integer>
                          (
                            Util.<T>getFromList
                            (
                              (List<Map<String,Object>>) p.first,
                              path.get(p.second),
                              evaluator != null ? evaluator : (a -> a)
                            ),
                            p.second
                          ) :
                          null
                      )
                  )
            )
        ).
        filter
        (
          pair ->
            !pair.isPresent() ||
              pair.map(p -> p.second == Integer.MAX_VALUE).orElse(false)
        ).
        map(pair -> pair.map(p -> (T) p.first)).
        findFirst().
        orElse(Optional.empty());
  }



  public static Stream<String>
  readLineConfig(final InputStream in) throws IOException
  {
    return
      readLineConfig
      (
        new BufferedReader
        (
          new EscapedUnicodeFilterReader(new InputStreamReader(in, "UTF-8"))
        )
      );
  }



  public static Stream<String>
  readLineConfig(final BufferedReader in) throws IOException
  {
    return readLineConfig(in.lines());
  }



  public static Stream<String>
  readLineConfig(final Path path) throws IOException
  {
    return readLineConfig(Files.lines(path, Charset.forName("UTF-8")));
  }



  /**
   * Returns lines from a stream of strings. Comments start with the "#"
   * character and are removed from the result. Lines can be split over
   * multiple lines with the "\" character. The pieces will be assembled into
   * a single line.
   * @param lines the given stream of lines.
   * @return The generated lines.
   */

  public static Stream<String>
  readLineConfig(final Stream<String> lines)
  {
    final List<String> buffer = new ArrayList<>();

    return
      lines.
        map
        (
          line ->
            (
              line.indexOf('#') != -1 ?
                line.substring(0, line.indexOf('#')) : line
            ).trim()
        ).
        filter(line -> line.length() > 0).
        map
        (
          line ->
            line.charAt(line.length() - 1) == '\\' ?
              SideEffect.<String>run
              (
                () -> buffer.add(line.substring(0, line.length() - 1))
              ).andThenGet(() -> null) :
              (flushLines(buffer) + line)
        ).
        filter(Objects::nonNull);
  }



  public static <T> Stream<T>
  stream(final Iterator<T> iterator)
  {
    return
      StreamSupport.stream
      (
        Spliterators.spliteratorUnknownSize
        (
          iterator,
          Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE
        ),
        false
      );
  }



  public static <T> Stream<T>
  stream(final Enumeration<T> enumeration)
  {
    return
      stream
      (
        new Iterator<T>()
        {
          public boolean
          hasNext()
          {
            return enumeration.hasMoreElements();
          }

          public T
          next()
          {
            return enumeration.nextElement();
          }
        }
      );
  }



  private static String
  stripCondition(final String field)
  {
    final int index = field.indexOf('[');

    return index != -1 ? field.substring(0, index) : field;
  }



  /**
   * Iterates sequentially until the predicate returns <code>false</code>.
   * @param seed the initial value.
   * @param f the function that calculates the next value.
   * @param p the predicate.
   * @param <T> the value type.
   * @return The generated stream.
   */

  public static <T> Stream<T>
  takeWhile(final T seed, final UnaryOperator<T> f, final Predicate<T> p)
  {
    return
      stream
      (
        new Iterator<T>()
        {
          private T current = seed;

          public boolean
          hasNext()
          {
            return p.test(current);
          }

          public T
          next()
          {
            final T result = current;

            current = f.apply(current);

            return result;
          }
        }
      );
  }



  public static boolean
  tryToDo(final RunnableWithException run)
  {
    return tryToDo(run, null);
  }



  public static boolean
  tryToDo(final RunnableWithException run, final Consumer<Exception> error)
  {
    try
    {
      run.run();

      return true;
    }

    catch (Exception e)
    {
      handleException(e, error);

      return false;
    }
  }



  public static void
  tryToDoRethrow(final RunnableWithException run)
  {
    tryToDo(run, e -> {throw new RuntimeException(e);});
  }



  public static boolean
  tryToDoSilent(final RunnableWithException run)
  {
    return tryToDo(run, e -> {});
  }



  public static <T> boolean
  tryToDoWith
  (
    final SupplierWithException<T> resource,
    final ConsumerWithException<T> fn
  )
  {
    return tryToDoWith(resource, fn, null);
  }



  public static <T> boolean
  tryToDoWith
  (
    final SupplierWithException<T> resource,
    final ConsumerWithException<T> fn,
    final Consumer<Exception> error
  )
  {
    try (final AutoCloseable r = (AutoCloseable) resource.get())
    {
      fn.accept((T) r);

      return true;
    }

    catch (Exception e)
    {
      handleException(e, error);

      return false;
    }
  }



  public static <T> boolean
  tryToDoWithRethrow
  (
    final SupplierWithException<T> resource,
    final ConsumerWithException<T> fn
  )
  {
    return tryToDoWith(resource, fn, e -> {throw new RuntimeException(e);});
  }



  public static <T> boolean
  tryToDoWithSilent
  (
    final SupplierWithException<T> resource,
    final ConsumerWithException<T> fn
  )
  {
    return tryToDoWith(resource, fn, e -> {});
  }



  public static <T> Optional<T>
  tryToGet(final SupplierWithException<T> run)
  {
    return tryToGet(run, null);
  }



  public static <T> Optional<T>
  tryToGet
  (
    final SupplierWithException<T> run,
    final Function<Exception,T> error
  )
  {
    try
    {
      return Optional.ofNullable(run.get());
    }

    catch (Exception e)
    {
      return handleException(e, error);
    }
  }



  public static <T> Optional<T>
  tryToGetRethrow(final SupplierWithException<T> run)
  {
    return tryToGet(run, e -> {throw new RuntimeException(e);});
  }



  public static <T> Optional<T>
  tryToGetSilent(final SupplierWithException<T> run)
  {
    return tryToGet(run, e -> null);
  }



  public static <T,R> Optional<R>
  tryToGetWith
  (
    final SupplierWithException<T> resource,
    final FunctionWithException<T,R> fn
  )
  {
    return tryToGetWith(resource, fn, null);
  }



  /**
   * Tries to calculate a result with a resource that should implement
   * {@link java.lang.AutoCloseable}.
   * @param resource the function that produces the resource.
   * @param fn the function that calculates the result with the resource. It
   *           may return <code>null</code>.
   * @param error the exception handler.
   * @param <T> the resource type.
   * @param <R> the result type.
   * @return The optional value.
   */

  public static <T,R> Optional<R>
  tryToGetWith
  (
    final SupplierWithException<T> resource,
    final FunctionWithException<T,R> fn,
    final Function<Exception,R> error
  )
  {
    try (final AutoCloseable r = (AutoCloseable) resource.get())
    {
      return Optional.ofNullable(fn.apply((T) r));
    }

    catch (Exception e)
    {
      return handleException(e, error);
    }
  }



  public static <T,R> Optional<R>
  tryToGetWithRethrow
  (
    final SupplierWithException<T> resource,
    final FunctionWithException<T,R> fn
  )
  {
    return tryToGetWith(resource, fn, e -> {throw new RuntimeException(e);});
  }



  public static <T,R> Optional<R>
  tryToGetWithSilent
  (
    final SupplierWithException<T> resource,
    final FunctionWithException<T,R> fn
  )
  {
    return tryToGetWith(resource, fn, e -> null);
  }



  private static class AutoCloseList<T> implements AutoCloseCollection<T>

  {

    final private ConsumerWithException<T> close;
    final private List<AutoCloseable> list = new ArrayList<>();



    private
    AutoCloseList(final ConsumerWithException<T> close)
    {
      this.close = close;
    }



    public <U> U
    add(final U element, final ConsumerWithException<U> close)
    {
      list.add(autoClose(element, close));

      return element;
    }



    public T
    add(final T element)
    {
      if (!(element instanceof AutoCloseable) && close == null)
      {
        throw new RuntimeException("No close function.");
      }

      list.add
      (
        element instanceof AutoCloseable ?
          (AutoCloseable) element : autoClose(element, close)
      );

      return element;
    }



    public void
    close() throws Exception
    {
      list.forEach(e -> tryToDo(e::close));
    }

  } // AutoCloseList

} // Util
