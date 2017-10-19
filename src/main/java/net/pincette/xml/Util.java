package net.pincette.xml;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.Util.takeWhile;

import net.pincette.util.Pair;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.helpers.AttributesImpl;



/**
 * @author Werner Donn\u00e9
 */

public class Util

{

  public static Stream<Element>
  ancestors(final Node node)
  {
    return
      takeWhile
      (
        node.getParentNode(),
        Node::getParentNode,
        n -> n instanceof Element
      ).
      map(n -> (Element) n);
  }



  public static Stream<Attr>
  attributes(final Node node)
  {
    return stream(node.getAttributes()).map(n -> (Attr) n);
  }



  public static Stream<Attribute>
  attributes(final StartElement event)
  {
    return
      net.pincette.util.Util.
        stream((Iterator<Attribute>) event.getAttributes());
  }



  public static Stream<Node>
  documentOrder(final Node node)
  {
    return
      takeWhile
      (
        node,
        n ->
          n.getFirstChild() != null ?
            n.getFirstChild() :
            (
              n.getNextSibling() != null ?
                n.getNextSibling() : findNextHigherSibling(n)
            ),
        Objects::nonNull
      );
  }



  private static Node
  findNextHigherSibling(final Node node)
  {
    return
      node.getParentNode() != null ?
        Optional.ofNullable(node.getParentNode().getNextSibling()).
          orElse(findNextHigherSibling(node.getParentNode())) :
        null;
  }



  public static Optional<Pair<Integer,AttributesImpl>>
  getIndex
  (
    final AttributesImpl atts,
    final String namespaceUri,
    final String localName
  )
  {
    return
      Optional.of(atts.getIndex(namespaceUri, localName)).
        filter(i -> i != -1).
        map(i -> pair(i, atts));
  }



  public static Stream<Node>
  stream(final NamedNodeMap map)
  {
    return takeWhile(0, i -> i + 1, i -> i < map.getLength()).map(map::item);
  }

} // Util
