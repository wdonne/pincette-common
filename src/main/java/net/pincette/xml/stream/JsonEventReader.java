package net.pincette.xml.stream;

import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.isNameChar;
import static net.pincette.xml.Util.isNameStartChar;

import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Converts a JSON-stream in an XML-stream. The provided parser will be closed when this reader is
 * closed.
 *
 * @author Werner Donn\u00e9
 */
public class JsonEventReader implements XMLEventReader {

  private final Deque<Event> events = new LinkedList<>();
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private final Deque<String> keyNames = new LinkedList<>();
  private final JsonParser parser;
  private final Queue<XMLEvent> queue = new LinkedList<>();
  private boolean endDocument;
  private boolean parserEvent;
  private boolean startDocument;

  public JsonEventReader(final JsonParser parser) {
    this.parser = parser;
    keyNames.push("doc");
  }

  private static String cleanName(final String name) {
    return name.replace("$", "");
  }

  private static String toXmlName(final String name) {
    final char[] c = name.toCharArray();

    for (int i = 0; i < c.length; ++i) {
      c[i] = isNameChar(c[i]) ? c[i] : '-';
    }

    return (c.length > 0 && isNameStartChar(c[0]) ? "" : "_") + new String(c);
  }

  public void close() {
    parser.close();
  }

  private XMLEvent createEnd(final String name) {
    return factory.createEndElement("", "", toXmlName(name));
  }

  private XMLEvent createNop() {
    return factory.createCharacters("");
  }

  private XMLEvent createStart(final String name) {
    return factory.createStartElement("", "", toXmlName(name));
  }

  private XMLEvent createValue(final String value) {
    final String name = events.peek() == JsonParser.Event.START_ARRAY ? "value" : keyNames.peek();
    final XMLEvent result = createStart(name);

    queue.add(factory.createCharacters(value));
    queue.add(createEnd(name));

    return result;
  }

  public String getElementText() throws XMLStreamException {
    throw new XMLStreamException("Not supported");
  }

  public Object getProperty(final String name) {
    throw new IllegalArgumentException("Property " + name + " is not supported");
  }

  private XMLEvent handleEndObject() {
    events.pop();

    final Event parent = events.isEmpty() ? null : events.peek();

    return createEnd(parent == Event.START_ARRAY ? "object" : keyNames.pop());
  }

  private XMLEvent handleEvent(final Event event) {
    switch (event) {
      case END_ARRAY:
      case END_OBJECT:
        return handleEndObject();

      case KEY_NAME:
        keyNames.push(cleanName(parser.getString()));
        return createNop();

      case START_ARRAY:
      case START_OBJECT:
        return handleStartObject(event);

      case VALUE_FALSE:
        return createValue("false");

      case VALUE_NUMBER:
        return createValue(
            parser.isIntegralNumber()
                ? String.valueOf(parser.getLong())
                : parser.getBigDecimal().toString());

      case VALUE_TRUE:
        return createValue("true");
      case VALUE_STRING:
        return createValue(parser.getString());

      default:
        return createNop();
    }
  }

  private XMLEvent handleStartObject(final Event event) {
    final Event parent = events.isEmpty() ? null : events.peek();

    events.push(event);

    return createStart(parent == Event.START_ARRAY ? "object" : keyNames.peek());
  }

  public boolean hasNext() {
    parserEvent = false;

    return !startDocument || queue.peek() != null || hasNextParserEvent() || !endDocument;
  }

  private boolean hasNextParserEvent() {
    parserEvent = parser.hasNext();

    return parserEvent;
  }

  public XMLEvent next() {
    return tryToGetRethrow(this::nextEvent).orElseThrow(NoSuchElementException::new);
  }

  public XMLEvent nextEvent() throws XMLStreamException {
    if (!startDocument) {
      startDocument = true;

      return factory.createStartDocument();
    }

    if (queue.peek() != null) {
      return queue.poll();
    }

    if (parserEvent) {
      return handleEvent(parser.next());
    }

    if (!endDocument) {
      endDocument = true;

      return factory.createEndDocument();
    }

    throw new XMLStreamException("No more JSON events");
  }

  public XMLEvent nextTag() throws XMLStreamException {
    throw new XMLStreamException("Not supported");
  }

  public XMLEvent peek() {
    return !startDocument ? factory.createStartDocument() : peekNextAfterStart();
  }

  private XMLEvent peekEnd() {
    return !endDocument ? factory.createEndDocument() : null;
  }

  private XMLEvent peekNextAfterStart() {
    return queue.peek() != null ? queue.peek() : peekNotQueued();
  }

  private XMLEvent peekNotQueued() {
    return parserEvent ? peekParserEvent() : peekEnd();
  }

  private XMLEvent peekParserEvent() {
    parserEvent = false;

    final XMLEvent event = handleEvent(parser.next());

    queue.add(event);

    return event;
  }
}
