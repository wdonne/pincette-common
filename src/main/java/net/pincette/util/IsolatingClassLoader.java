package net.pincette.util;

import net.pincette.function.SideEffect;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.pincette.util.Collections.set;
import static net.pincette.util.Collections.union;
import static net.pincette.util.Util.tryToGetRethrow;



/**
 * This class loader doesn't delegate class loading to the parent unless the
 * name has one of the given prefixes. The parent class loader's
 * <code>findClass</code> method is used to actually load the class.
 * @author Werner Donn\u00e9
 */

public class IsolatingClassLoader extends ClassLoader

{

  final private static Set<String> SYSTEM_PREFIXES =
    set
    (
      "int", "char", "void", "long", "short", "double", "byte", "float",
        "boolean", "java.", "javax.", "org.ietf.jgss.", "org.omg.",
        "org.w3c.dom.", "org.xml.sax."
    );

  final Set<String> delegatedPrefixes;
  final Map<String,Class<?>> loaded = new ConcurrentHashMap<>();



  public
  IsolatingClassLoader
  (
    final Set<String> delegatedPrefixes,
    final ClassLoader parent
  )
  {
    super(parent);
    this.delegatedPrefixes = union(SYSTEM_PREFIXES, delegatedPrefixes);
  }




  protected Class<?>
  loadClass(final String name, final boolean resolve)
    throws ClassNotFoundException
  {
     return
       shouldDelegate(name) ?
         super.loadClass(name, resolve) : loadLocally(name, resolve);
  }



  private Class<?>
  loadLocally(final String name, final boolean resolve)
    throws ClassNotFoundException
  {
    return
      loaded.computeIfAbsent
      (
        name,
        k ->
          tryToGetRethrow(() -> super.findClass(name)).
            map
            (
              c ->
                resolve ?
                  SideEffect.<Class<?>>run(() -> resolveClass(c)).
                    andThenGet(() -> c) :
                  c
            ).
            orElse(null)
      );
  }



  private boolean
  shouldDelegate(final String name)
  {
    return delegatedPrefixes.stream().anyMatch(name::startsWith);
  }

} // IsolatingClassLoader
