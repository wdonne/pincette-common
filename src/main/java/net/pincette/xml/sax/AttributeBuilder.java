package net.pincette.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A chained builder voor SAX attribute sets.
 *
 * @author Werner Donn\u00e9
 */
public class AttributeBuilder {

  private final AttributesImpl atts;

  public AttributeBuilder() {
    this(new AttributesImpl());
  }

  public AttributeBuilder(final Attributes atts) {
    this(new AttributesImpl(atts));
  }

  public AttributeBuilder(final AttributesImpl atts) {
    this.atts = atts;
  }

  public AttributeBuilder add(
      final String uri,
      final String localName,
      final String qName,
      final String type,
      final String value) {
    atts.addAttribute(uri, localName, qName, type, value);

    return this;
  }

  public Attributes build() {
    return atts;
  }

  public AttributeBuilder remove(final int index) {
    atts.removeAttribute(index);

    return this;
  }

  public AttributeBuilder set(
      final int index,
      final String uri,
      final String localName,
      final String qName,
      final String type,
      final String value) {
    atts.setAttribute(index, uri, localName, qName, type, value);

    return this;
  }

  public AttributeBuilder set(final Attributes atts) {
    this.atts.setAttributes(atts);

    return this;
  }

  public AttributeBuilder setLocalName(final int index, final String localName) {
    atts.setLocalName(index, localName);

    return this;
  }

  public AttributeBuilder setQName(final int index, final String qName) {
    atts.setQName(index, qName);

    return this;
  }

  public AttributeBuilder setType(final int index, final String type) {
    atts.setType(index, type);

    return this;
  }

  public AttributeBuilder setURI(final int index, final String uri) {
    atts.setURI(index, uri);

    return this;
  }

  public AttributeBuilder setValue(final int index, final String value) {
    atts.setValue(index, value);

    return this;
  }
}
