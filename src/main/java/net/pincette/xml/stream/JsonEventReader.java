package net.pincette.xml.stream;

import javax.json.stream.JsonParser;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;



/**
 * Converts a JSON-stream in an XML-stream. The provided parser will be
 * closed when this reader is closed.
 * @author Werner Donn\u00e9
 */

public class JsonEventReader implements XMLEventReader

{

  private boolean endDocument;
  private final Stack<JsonParser.Event> events = new Stack<>();
  private final XMLEventFactory factory = XMLEventFactory.newFactory();
  private final Stack<String> keyNames = new Stack<>();
  private final JsonParser parser;
  private boolean parserEvent;
  private final Queue<XMLEvent> queue = new LinkedList<>();
  private boolean startDocument;



  public
  JsonEventReader(final JsonParser parser)
  {
    this.parser = parser;
    keyNames.push("doc");
  }



  private static String
  cleanName(final String name)
  {
    return name.replace("$", "");
  }



  public void
  close() throws XMLStreamException
  {
    parser.close();
  }



  private XMLEvent
  createEnd(final String name)
  {
    return factory.createEndElement("", "", name);
  }



  private XMLEvent
  createNop()
  {
    return factory.createCharacters("");
  }



  private XMLEvent
  createStart(final String name)
  {
    return factory.createStartElement("", "", name);
  }



  private XMLEvent
  createValue(final String value)
  {
    final String name =
      events.peek() == JsonParser.Event.START_ARRAY ? "value" : keyNames.peek();
    final XMLEvent result = createStart(name);

    queue.add(factory.createCharacters(value));
    queue.add(createEnd(name));

    return result;
  }



  public String
  getElementText() throws XMLStreamException
  {
    throw new XMLStreamException("Not supported");
  }



  public Object
  getProperty(final String name)
  {
    throw
      new IllegalArgumentException("Property " + name + " is not supported");
  }



  private XMLEvent
  handleEvent(final JsonParser.Event event)
  {
    switch (event)
    {
      case END_ARRAY: case END_OBJECT:
      {
        events.pop();

        final JsonParser.Event parent =
          events.empty() ? null : events.peek();

        return
          createEnd
          (
            parent == JsonParser.Event.START_ARRAY ? "object" : keyNames.pop()
          );
      }

      case KEY_NAME:
        keyNames.push(cleanName(parser.getString()));
        return createNop();

      case START_ARRAY: case START_OBJECT:
      {
        final JsonParser.Event parent =
          events.empty() ? null : events.peek();

        events.push(event);

        return
          createStart
          (
            parent == JsonParser.Event.START_ARRAY ? "object" : keyNames.peek()
          );
      }

      case VALUE_FALSE: return createValue("false");

      case VALUE_NUMBER:
        return
          createValue
          (
            parser.isIntegralNumber() ?
              String.valueOf(parser.getLong()) :
              parser.getBigDecimal().toString()
          );

      case VALUE_TRUE: return createValue("true");
      case VALUE_STRING: return createValue(parser.getString());

      default: return createNop();
    }
  }



  public boolean
  hasNext()
  {
    parserEvent = false;

    return
      !startDocument || queue.peek() != null ||
        (parserEvent = parser.hasNext()) || !endDocument;
  }



  public XMLEvent
  next()
  {
    try
    {
      return nextEvent();
    }

    catch (XMLStreamException e)
    {
      throw new RuntimeException(e);
    }
  }



  public XMLEvent
  nextEvent() throws XMLStreamException
  {
    if (!startDocument)
    {
      startDocument = true;

      return factory.createStartDocument();
    }

    if (queue.peek() != null)
    {
      return queue.poll();
    }

    if (parserEvent)
    {
      return handleEvent(parser.next());
    }

    if (!endDocument)
    {
      endDocument = true;

      return factory.createEndDocument();
    }

    throw new XMLStreamException("No more JSON events");
  }



  public XMLEvent
  nextTag() throws XMLStreamException
  {
    throw new XMLStreamException("Not supported");
  }



  public XMLEvent
  peek() throws XMLStreamException
  {
    return
      !startDocument ?
        factory.createStartDocument() :
        (
          queue.peek() != null ?
            queue.peek() :
            (
              parserEvent ?
                peekParserEvent() :
                (!endDocument ? factory.createEndDocument(): null)
            )
        );
  }



  private XMLEvent
  peekParserEvent()
  {
    parserEvent = false;

    final XMLEvent event = handleEvent(parser.next());

    queue.add(event);

    return event;
  }

} // JsonEventReader
