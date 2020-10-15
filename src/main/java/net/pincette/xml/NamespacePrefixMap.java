package net.pincette.xml;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.xml.namespace.NamespaceContext;

/**
 * This class copes with the fact that the same namespace declarations can be nested and that
 * prefixes can be redeclared for other URIs.
 *
 * @author Werner Donn\u00e9
 */
public class NamespacePrefixMap implements NamespaceContext {
  private final Map<String, Deque<String>> namespaceMap = new HashMap<>();
  private final Map<String, Deque<String>> prefixMap = new HashMap<>();

  private static void map(
      final Map<String, Deque<String>> mapping, final String from, final String to) {
    mapping.computeIfAbsent(from, k -> new ArrayDeque<>()).push(to);
  }

  private static void unmap(final Map<String, Deque<String>> mapping, final String from) {
    Optional.ofNullable(mapping.get(from))
        .ifPresent(
            stack -> {
              stack.pop();

              if (stack.isEmpty()) {
                mapping.remove(from);
              }
            });
  }

  public void endPrefixMapping(final String prefix) {
    Optional.ofNullable(prefixMap.get(prefix))
        .ifPresent(
            stack -> {
              unmap(namespaceMap, stack.peek());
              unmap(prefixMap, prefix);
            });
  }

  public Map<String, String> getCurrentPrefixMap() {
    return prefixMap.entrySet().stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                e -> Optional.ofNullable(e.getValue()).map(Deque::peek).orElse("")));
  }

  /**
   * Returns the most recent namespace prefix which is associated with <code>uri</code> or <code>
   * null</code> if there isn't any.
   *
   * @param uri the given URI.
   * @return The prefix.
   */
  public String getNamespacePrefix(final String uri) {
    if (uri == null || "".equals(uri)) {
      throw new IllegalArgumentException();
    }

    final Supplier<String> attrNsOr =
        () ->
            XMLNS_ATTRIBUTE_NS_URI.equals(uri)
                ? XMLNS_ATTRIBUTE
                : Optional.ofNullable(namespaceMap.get(uri)).map(Deque::peek).orElse(null);

    return XML_NS_URI.equals(uri) ? XML_NS_PREFIX : attrNsOr.get();
  }

  /** Returns <code>null</code> if there is no mapping. */
  public String getNamespaceURI(final String prefix) {
    if (prefix == null) {
      throw new IllegalArgumentException();
    }

    final Supplier<String> attrNsOr =
        () ->
            XMLNS_ATTRIBUTE.equals(prefix)
                ? XMLNS_ATTRIBUTE_NS_URI
                : Optional.ofNullable(prefixMap.get(prefix)).map(Deque::peek).orElse(null);

    return XML_NS_PREFIX.equals(prefix) ? XML_NS_URI : attrNsOr.get();
  }

  public String getPrefix(final String namespaceURI) {
    return getNamespacePrefix(namespaceURI);
  }

  public Iterator<String> getPrefixes(final String namespaceURI) {
    if (namespaceURI == null || "".equals(namespaceURI)) {
      throw new IllegalArgumentException();
    }

    final Supplier<Collection<String>> mapOr =
        () ->
            namespaceMap.get(namespaceURI) != null
                ? namespaceMap.get(namespaceURI)
                : new ArrayList<>();
    final Supplier<Collection<String>> attrNsOr =
        () ->
            XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)
                ? singletonList(XMLNS_ATTRIBUTE)
                : mapOr.get();

    return (XML_NS_URI.equals(namespaceURI) ? singletonList(XML_NS_PREFIX) : attrNsOr.get())
        .iterator();
  }

  public void startPrefixMapping(final String prefix, final String uri) {
    map(prefixMap, prefix, uri);
    map(namespaceMap, uri, prefix);
  }
}
