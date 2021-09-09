package net.pincette.util;

import static java.util.Arrays.stream;
import static java.util.logging.Logger.getLogger;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Collections.computeIfAbsent;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetWith;
import static net.pincette.util.Util.tryToGetWithRethrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipFile;
import net.pincette.cls.ClassFile;
import net.pincette.cls.Field;
import net.pincette.cls.LocalVariable;
import net.pincette.cls.Method;
import net.pincette.function.SideEffect;

/**
 * With this class loader you can load classes in isolation. The classes are actually loaded as
 * resources in order to reuse the parent class loader mechanisms for fetching the byte array.
 *
 * @author Werner Donn\u00e9
 */
public class IsolatingClassLoader extends ClassLoader {
  private static final Map<ClassLoader, Map<String, byte[]>> classesPerParent = new HashMap<>();
  private static final String[] defaultPrefixes = {
    "int", "char", "void", "long", "short", "double", "byte", "float", "boolean", "java.", "javax."
  };
  private static final String[] excludePrefixes = {"javax.xml.stream.", "javax.xml.namespace."};

  private final File[] classPath;
  private final Map<String, Class<?>> loadedClasses = new HashMap<>();
  private final ClassLoader parent;
  private final Set<String> parentClasses = new HashSet<>();
  private final String[] prefixesForParent;
  private final String[] prefixesNotForParent;

  public IsolatingClassLoader() {
    this(new String[0], null);
  }

  public IsolatingClassLoader(final ClassLoader parent) {
    this(new String[0], parent);
  }

  public IsolatingClassLoader(final String[] parentClasses) {
    this(parentClasses, null);
  }

  public IsolatingClassLoader(final String[] parentClasses, final ClassLoader parent) {
    this(parentClasses, new String[0], new String[0], parent);
  }

  public IsolatingClassLoader(
      final String[] parentClasses,
      final String[] prefixesForParent,
      final String[] prefixesNotForParent,
      final ClassLoader parent) {
    this(parentClasses, prefixesForParent, prefixesNotForParent, parent, new File[0]);
  }

  /**
   * @param parentClasses classes which are to be loaded with the parent class loader.
   * @param prefixesForParent classes that have one of the prefixes in their name are loaded with
   *     the parent class loader unless they also have one of the prefixes in <code>
   *     prefixesNotForParent</code>.
   * @param prefixesNotForParent see <code>prefixesForParent</code>.
   * @param parent this class loader is only used for loading the classes in <code>parentClasses
   *     </code>, with their inferred classes, as well as resources. Note that the <code>getParent
   *     </code> method will always return <code>null</code>, no matter the value of <code>parent
   *     </code>.
   * @param classPath this path is searched for class files that are not for the parent class loader
   *     and for resources. If nothing is found in the path the parent class loader will be
   *     consulted. The parameter may contains directories as well as JAR-files.
   */
  public IsolatingClassLoader(
      final String[] parentClasses,
      final String[] prefixesForParent,
      final String[] prefixesNotForParent,
      final ClassLoader parent,
      final File[] classPath) {
    super(null);
    this.parent = parent != null ? parent : getSystemClassLoader();
    this.prefixesForParent = new String[defaultPrefixes.length + prefixesForParent.length];
    this.prefixesNotForParent = new String[excludePrefixes.length + prefixesNotForParent.length];
    this.classPath = classPath;

    System.arraycopy(defaultPrefixes, 0, this.prefixesForParent, 0, defaultPrefixes.length);

    System.arraycopy(
        prefixesForParent,
        0,
        this.prefixesForParent,
        defaultPrefixes.length,
        prefixesForParent.length);

    System.arraycopy(excludePrefixes, 0, this.prefixesNotForParent, 0, excludePrefixes.length);

    System.arraycopy(
        prefixesNotForParent,
        0,
        this.prefixesNotForParent,
        excludePrefixes.length,
        prefixesNotForParent.length);

    tryToDoRethrow(() -> inferClasses(parentClasses));
  }

  private static URL fromZip(final String name, final File classPathEntry) {
    return tryToGetWith(
            () -> new ZipFile(classPathEntry),
            zip ->
                Optional.ofNullable(zip.getEntry(name))
                    .flatMap(
                        e ->
                            tryToGetRethrow(
                                () -> new URL("jar:" + classPathEntry.toURI() + "!/" + name)))
                    .orElse(null))
        .orElse(null);
  }

  private static boolean hasPrefix(final String s, final String[] prefixes) {
    return stream(prefixes).anyMatch(s::startsWith);
  }

  private static boolean isJar(final File classPathEntry) {
    return classPathEntry.isFile() && classPathEntry.getName().endsWith(".jar");
  }

  private static URL resourceUrl(final String name, final File classPathEntry) {
    final Supplier<URL> fromDirectory =
        () ->
            classPathEntry.isDirectory() && new File(classPathEntry, name).exists()
                ? tryToGetRethrow(() -> new File(classPathEntry, name).toURI().toURL()).orElse(null)
                : null;

    return isJar(classPathEntry) ? fromZip(name, classPathEntry) : fromDirectory.get();
  }

  private static void trace(final String s) {
    getLogger("IsolatingClassLoader").finest(s);
  }

  private void definePackageWithName(final String name) {
    Optional.of(name.lastIndexOf('.'))
        .filter(index -> index != -1)
        .map(index -> name.substring(0, index))
        .filter(n -> getDefinedPackage(n) == null)
        .ifPresent(n -> definePackage(name, null, null, null, null, null, null, null));
  }

  private byte[] loadFromResource(final String name) {
    return tryToGetRethrow(() -> getResourceAsStream(name.replace('.', '/') + ".class"))
        .map(in -> pair(in, new ByteArrayOutputStream()))
        .map(
            pair ->
                SideEffect.<byte[]>run(() -> tryToDoRethrow(() -> copy(pair.first, pair.second)))
                    .andThenGet(pair.second::toByteArray))
        .orElse(null);
  }

  @Override
  protected Class<?> findClass(final String className) throws ClassNotFoundException {
    try {
      return !isNotForParent(className)
          ? SideEffect.<Class<?>>run(() -> trace(className + ": parent classloader"))
              .andThenGet(() -> tryToGetRethrow(() -> parent.loadClass(className)).orElse(null))
          : computeIfAbsent(
              loadedClasses,
              className,
              name -> tryToGetRethrow(() -> loadClassAsResource(name)).orElse(null));
    } catch (Exception e) {
      throw e.getCause() instanceof ClassNotFoundException
          ? (ClassNotFoundException) e.getCause()
          : new ClassNotFoundException("", e);
    }
  }

  private InputStream getClassStream(final String name) {
    return new ByteArrayInputStream(
        classesPerParent
            .computeIfAbsent(parent, p -> new HashMap<>())
            .computeIfAbsent(name, this::loadFromResource));
  }

  @Override
  public URL getResource(final String name) {
    return stream(classPath)
        .map(cp -> resourceUrl(name, cp))
        .filter(Objects::nonNull)
        .map(url -> SideEffect.<URL>run(() -> trace(name + ": " + url)).andThenGet(() -> url))
        .findFirst()
        .orElse(
            SideEffect.<URL>run(() -> trace(name + ": parent as resource"))
                .andThenGet(() -> parent.getResource(name)));
  }

  @Override
  public InputStream getResourceAsStream(final String name) {
    return Optional.ofNullable(getResource(name))
        .flatMap(r -> tryToGetRethrow(r::openStream))
        .orElse(null);
  }

  private void inferClasses(final String[] classes) {
    stream(classes)
        .map(c -> c.indexOf('[') != -1 ? c.substring(0, c.indexOf('[')) : c)
        .filter(this::isNotForParent)
        .map(name -> SideEffect.<String>run(() -> parentClasses.add(name)).andThenGet(() -> name))
        .forEach(
            name ->
                tryToGetWithRethrow(() -> getClassStream(name), ClassFile::parse)
                    .ifPresent(this::inferClasses));
  }

  private void inferClasses(final ClassFile classFile) {
    if (classFile.getSuperClassType() != null) {
      inferClasses(new String[] {classFile.getSuperClassType()});
    }

    inferClasses(classFile.getInterfaceTypes());
    inferFieldClasses(classFile.getFields());
    inferMethodClasses(classFile.getMethods());
  }

  private void inferFieldClasses(final Field[] fields) {
    stream(fields).forEach(f -> inferClasses(new String[] {f.getType()}));
  }

  private void inferMethodClasses(final Method[] methods) {
    stream(methods)
        .forEach(
            m -> {
              inferClasses(m.getExceptionTypes());
              inferClasses(m.getParameterTypes());
              inferClasses(new String[] {m.getReturnType()});

              if (m.getCode() != null) {
                inferVariableClasses(m.getCode().getLocalVariables());
              }
            });
  }

  private void inferVariableClasses(final LocalVariable[] variables) {
    stream(variables).forEach(v -> inferClasses(new String[] {v.getType()}));
  }

  private boolean isNotForParent(final String className) {
    return !parentClasses.contains(className)
        && (!hasPrefix(className, prefixesForParent) || hasPrefix(className, prefixesNotForParent));
  }

  @Override
  public Class<?> loadClass(final String name, final boolean resolve)
      throws ClassNotFoundException {
    final Class<?> c = findClass(name);

    if (resolve) {
      resolveClass(c);
    }

    return c;
  }

  private Class<?> loadClassAsResource(final String className) {
    definePackageWithName(className);

    return Optional.of(loadFromResource(className))
        .map(b -> defineClass(className, b, 0, b.length))
        .orElse(null);
  }
}
