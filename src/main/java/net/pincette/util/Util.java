package net.pincette.util;

import java.io.BufferedReader;
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
import net.pincette.function.ConsumerWithException;
import net.pincette.function.FunctionWithException;
import net.pincette.function.RunnableWithException;
import net.pincette.function.SideEffect;
import net.pincette.function.SupplierWithException;
import net.pincette.io.EscapedUnicodeFilterReader;



/**
 * General purpose utility functions.
 * @author Werner Donn\u00e9
 */

public class Util

{

  final private static Pattern EMAIL =
    Pattern.compile("[^@]+@[^@\\.]+\\.[^@]+");



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



  public static <T> AutoCloseable
  autoClose(final T resource, final ConsumerWithException<T> close)
  {
    return () -> close.accept(resource);
  }



  public static AutoCloseCollection
  autoClose()
  {
    return autoClose(null);
  }



  public static <T> AutoCloseCollection<T>
  autoClose(final ConsumerWithException<T> close)
  {
    return new AutoCloseList<T>(close);
  }



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



  public static Optional<String>
  getLastSegment(final String s, final String delimiter)
  {
    final List<String> segments = getSegments(s, delimiter).collect(toList());

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
   */

  public static Stream<String>
  getSegments(final String s, final String delimiter)
  {
    return Arrays.stream(s.split(delimiter)).filter(seg -> seg.length() > 0);
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
      Arrays.
        stream(tryToGet(() -> readLineConfig(in.get())).orElse(new String[0])).
        map(line -> line.split("=")).
        filter(line -> line.length == 2).
        collect(toMap(line -> line[0], line -> line[1]));
  }



  /**
   * The <code>path</code> is a dot-separated string.
   */

  public static <T> Optional<T>
  pathSearch(final Map<String,? extends T> map, final String path)
  {
    return pathSearch(map, path, null);
  }



  /**
   * The <code>path</code> is a dot-separated string.
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
            !pair.isPresent() ?
              Optional.empty() :
              (
                pair.get().second == path.size() - 1 ?
                  Optional.of(new Pair<>(pair.get().first, Integer.MAX_VALUE)) :
                  (
                    pair.get().first instanceof Map ?
                      Optional.of
                      (
                        new Pair<>
                        (
                          ((Map<String,Object>) pair.get().first).get
                          (
                            stripCondition(path.get(pair.get().second + 1))
                          ),
                          pair.get().second + 1
                        )
                      ) :
                      (
                        pair.get().first instanceof List ?
                          Optional.of
                          (
                            new Pair<>
                            (
                              Util.<T>getFromList
                              (
                                (List<Map<String,Object>>) pair.get().first,
                                path.get(pair.get().second),
                                evaluator != null ? evaluator : (a -> a)
                              ),
                              pair.get().second
                            )
                          ) :
                          Optional.empty()
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
        map
        (
          pair -> !pair.isPresent() ?
            Optional.<T>empty() : Optional.ofNullable((T) pair.get().first)
        ).
        findFirst().
        orElse(Optional.empty());
  }



  public static String[]
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



  public static String[]
  readLineConfig(final BufferedReader in) throws IOException
  {
    return readLineConfig(in.lines());
  }



  public static String[]
  readLineConfig(final Path path) throws IOException
  {
    return readLineConfig(Files.lines(path, Charset.forName("UTF-8")));
  }



  public static String[]
  readLineConfig(final Stream<String> lines) throws IOException
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
        filter(Objects::nonNull).
        toArray(String[]::new);
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
