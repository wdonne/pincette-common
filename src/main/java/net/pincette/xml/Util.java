package net.pincette.xml;

import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.takeWhile;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import net.pincette.util.Pair;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.helpers.AttributesImpl;

/** @author Werner Donn\u00e9 */
public class Util {

  private Util() {}

  public static Stream<Element> ancestors(final Node node) {
    return takeWhile(node.getParentNode(), Node::getParentNode, n -> n instanceof Element)
        .map(n -> (Element) n);
  }

  public static Stream<Attr> attributes(final Node node) {
    return stream(node.getAttributes()).map(n -> (Attr) n);
  }

  public static Stream<Attribute> attributes(final StartElement event) {
    return net.pincette.util.StreamUtil.stream((Iterator<Attribute>) event.getAttributes());
  }

  public static Stream<Node> documentOrder(final Node node) {
    final UnaryOperator<Node> nextSibling =
        n -> n.getNextSibling() != null ? n.getNextSibling() : findNextHigherSibling(n);

    return takeWhile(
        node,
        n -> n.getFirstChild() != null ? n.getFirstChild() : nextSibling.apply(n),
        Objects::nonNull);
  }

  private static Node findNextHigherSibling(final Node node) {
    return node.getParentNode() != null
        ? Optional.ofNullable(node.getParentNode().getNextSibling())
            .orElse(findNextHigherSibling(node.getParentNode()))
        : null;
  }

  public static Optional<Pair<Integer, AttributesImpl>> getIndex(
      final AttributesImpl atts, final String namespaceUri, final String localName) {
    return Optional.of(atts.getIndex(namespaceUri, localName))
        .filter(i -> i != -1)
        .map(i -> pair(i, atts));
  }

  @SuppressWarnings("squid:S3776") // Can't reduce complexity. It comes from the XML spec.
  public static boolean isCombiningChar(final char c) {
    return (c >= '\u0300' && c <= '\u0345')
        || (c >= '\u0360' && c <= '\u0361')
        || (c >= '\u0483' && c <= '\u0486')
        || (c >= '\u0591' && c <= '\u05a1')
        || (c >= '\u05a3' && c <= '\u05b9')
        || (c >= '\u05bb' && c <= '\u05bd')
        || c == '\u05bf'
        || (c >= '\u05c1' && c <= '\u05c2')
        || c == '\u05c4'
        || (c >= '\u064b' && c <= '\u0652')
        || c == '\u0670'
        || (c >= '\u06d6' && c <= '\u06dc')
        || (c >= '\u06dd' && c <= '\u06df')
        || (c >= '\u06e0' && c <= '\u06e4')
        || (c >= '\u06e7' && c <= '\u06e8')
        || (c >= '\u06ea' && c <= '\u06ed')
        || (c >= '\u0901' && c <= '\u0903')
        || c == '\u093c'
        || (c >= '\u093e' && c <= '\u094c')
        || c == '\u094d'
        || (c >= '\u0951' && c <= '\u0954')
        || (c >= '\u0962' && c <= '\u0963')
        || (c >= '\u0981' && c <= '\u0983')
        || c == '\u09bc'
        || c == '\u09be'
        || c == '\u09bf'
        || (c >= '\u09c0' && c <= '\u09c4')
        || (c >= '\u09c7' && c <= '\u09c8')
        || (c >= '\u09cb' && c <= '\u09cd')
        || c == '\u09d7'
        || (c >= '\u09e2' && c <= '\u09e3')
        || c == '\u0a02'
        || c == '\u0a3c'
        || c == '\u0a3e'
        || c == '\u0a3f'
        || (c >= '\u0a40' && c <= '\u0a42')
        || (c >= '\u0a47' && c <= '\u0a48')
        || (c >= '\u0a4b' && c <= '\u0a4d')
        || (c >= '\u0a70' && c <= '\u0a71')
        || (c >= '\u0a81' && c <= '\u0a83')
        || c == '\u0abc'
        || (c >= '\u0abe' && c <= '\u0ac5')
        || (c >= '\u0ac7' && c <= '\u0ac9')
        || (c >= '\u0acb' && c <= '\u0acd')
        || (c >= '\u0b01' && c <= '\u0b03')
        || c == '\u0b3c'
        || (c >= '\u0b3e' && c <= '\u0b43')
        || (c >= '\u0b47' && c <= '\u0b48')
        || (c >= '\u0b4b' && c <= '\u0b4d')
        || (c >= '\u0b56' && c <= '\u0b57')
        || (c >= '\u0b82' && c <= '\u0b83')
        || (c >= '\u0bbe' && c <= '\u0bc2')
        || (c >= '\u0bc6' && c <= '\u0bc8')
        || (c >= '\u0bca' && c <= '\u0bcd')
        || c == '\u0bd7'
        || (c >= '\u0c01' && c <= '\u0c03')
        || (c >= '\u0c3e' && c <= '\u0c44')
        || (c >= '\u0c46' && c <= '\u0c48')
        || (c >= '\u0c4a' && c <= '\u0c4d')
        || (c >= '\u0c55' && c <= '\u0c56')
        || (c >= '\u0c82' && c <= '\u0c83')
        || (c >= '\u0cbe' && c <= '\u0cc4')
        || (c >= '\u0cc6' && c <= '\u0cc8')
        || (c >= '\u0cca' && c <= '\u0ccd')
        || (c >= '\u0cd5' && c <= '\u0cd6')
        || (c >= '\u0d02' && c <= '\u0d03')
        || (c >= '\u0d3e' && c <= '\u0d43')
        || (c >= '\u0d46' && c <= '\u0d48')
        || (c >= '\u0d4a' && c <= '\u0d4d')
        || c == '\u0d57'
        || c == '\u0e31'
        || (c >= '\u0e34' && c <= '\u0e3a')
        || (c >= '\u0e47' && c <= '\u0e4e')
        || c == '\u0eb1'
        || (c >= '\u0eb4' && c <= '\u0eb9')
        || (c >= '\u0ebb' && c <= '\u0ebc')
        || (c >= '\u0ec8' && c <= '\u0ecd')
        || (c >= '\u0f18' && c <= '\u0f19')
        || c == '\u0f35'
        || c == '\u0f37'
        || c == '\u0f39'
        || c == '\u0f3e'
        || c == '\u0f3f'
        || (c >= '\u0f71' && c <= '\u0f84')
        || (c >= '\u0f86' && c <= '\u0f8b')
        || (c >= '\u0f90' && c <= '\u0f95')
        || c == '\u0f97'
        || (c >= '\u0f99' && c <= '\u0fad')
        || (c >= '\u0fb1' && c <= '\u0fb7')
        || c == '\u0fb9'
        || (c >= '\u20d0' && c <= '\u20dc')
        || c == '\u20e1'
        || (c >= '\u302a' && c <= '\u302f')
        || c == '\u3099'
        || c == '\u309a';
  }

  public static boolean isExtenderChar(final char c) {
    return c == '\u00b7'
        || c == '\u02d0'
        || c == '\u02d1'
        || c == '\u0387'
        || c == '\u0640'
        || c == '\u0e46'
        || c == '\u0ec6'
        || c == '\u3005'
        || (c >= '\u3031' && c <= '\u3035')
        || (c >= '\u309d' && c <= '\u309e')
        || (c >= '\u30fc' && c <= '\u30fe');
  }

  public static boolean isNameChar(final char c) {
    return Character.isLetterOrDigit(c)
        || c == '.'
        || c == '-'
        || c == '_'
        || c == ':'
        || isCombiningChar(c)
        || isExtenderChar(c);
  }

  public static boolean isName(String s) {
    return s.length() > 0
        && isNameStartChar(s.charAt(0))
        && s.chars().allMatch(c -> isNameChar((char) c));
  }

  public static boolean isNameStartChar(final char c) {
    return Character.isLetter(c) || c == '_' || c == ':';
  }

  public static boolean isXml(final String mimeType) {
    return Optional.of(mimeType)
        .map(m -> stripMimeTypeParameters(m).toLowerCase())
        .map(m -> "text/xml".equals(m) || "application/xml".equals(m) || m.endsWith("+xml"))
        .orElse(false);
  }

  public static boolean isXmlChar(final char c) {
    return c == 0x9
        || c == 0xa
        || c == 0xd
        || (c >= 0x20 && c <= 0xd7ff)
        || (c >= 0xe000 && c <= 0xfffd);
  }

  public static boolean isXmlText(final String s) {
    return s.chars().allMatch(c -> isXmlChar((char) c));
  }

  public static boolean isXmlText(char[] c) {
    return isXmlText(new String(c));
  }

  public static Stream<Node> stream(final NamedNodeMap map) {
    return takeWhile(0, i -> i + 1, i -> i < map.getLength()).map(map::item);
  }

  private static String stripMimeTypeParameters(final String mimeType) {
    return Optional.of(mimeType.indexOf(';'))
        .filter(index -> index != -1)
        .map(index -> mimeType.substring(0, index))
        .orElse(mimeType);
  }
}
