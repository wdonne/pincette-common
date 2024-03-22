package net.pincette.xml;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Util.isUri;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.resolveSystemId;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.xml.stream.XMLResolver;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * This entity resolver uses a catalog as defined by SGML Open Technical Resolution TR9401:1997.
 * Only PUBLIC and SYSTEM statements are supported at this time. Relative URLs are resolved using
 * the catalog URL as the base URL.
 *
 * @author Werner Donné
 */
public class CatalogResolver implements EntityResolver, XMLResolver {
  // Alphabet.

  private static final int SINGLE_QUOTE = 0;
  private static final int DOUBLE_QUOTE = 1;
  private static final int OTHER = 2;
  private static final int SPACE = 3;
  private static final int WHITE = 4;
  private static final int EOF = 5;

  // States.

  private static final int TYP = 0;
  private static final int SQ1 = 1;
  private static final int DQ1 = 2;
  private static final int ID1 = 3;
  private static final int SQ2 = 4;
  private static final int DQ2 = 5;
  private static final int ERR = 6;

  private static final int[][][] FSM = {
    {{SQ1, 1}, {DQ1, 1}, {TYP, 0}, {TYP, 0}, {TYP, 0}, {TYP, 0}}, // TYP
    {{ID1, 1}, {SQ1, 0}, {SQ1, 0}, {SQ1, 0}, {ERR, 0}, {ERR, 0}}, // SQ1
    {{DQ1, 0}, {ID1, 1}, {DQ1, 0}, {DQ1, 0}, {ERR, 0}, {ERR, 0}}, // DQ1
    {{SQ2, 1}, {DQ2, 1}, {ERR, 0}, {ID1, 0}, {ID1, 0}, {ERR, 0}}, // ID1
    {{TYP, 1}, {SQ2, 0}, {SQ2, 0}, {SQ2, 0}, {ERR, 0}, {ERR, 0}}, // SQ2
    {{DQ2, 0}, {TYP, 1}, {DQ2, 0}, {DQ2, 0}, {ERR, 0}, {ERR, 0}} // DQ2
  };

  private final String catalogSystemId;
  private final Map<String, String> publicIdentifiers = new HashMap<>();
  private final Map<String, String> systemIdentifiers = new HashMap<>();

  public CatalogResolver(final URL catalogUrl) throws IOException {
    this(catalogUrl.toString(), null);
  }

  public CatalogResolver(final String catalogSystemId) throws IOException {
    this(catalogSystemId, null);
  }

  public CatalogResolver(final URL catalogUrl, final InputStream in) throws IOException {
    this(catalogUrl.toString(), in);
  }

  public CatalogResolver(final String catalogSystemId, final InputStream in) throws IOException {
    final Supplier<InputStream> tryCatalog =
        () ->
            tryToGetRethrow(
                    () ->
                        isUri(catalogSystemId)
                            ? new URL(catalogSystemId).openStream()
                            : new FileInputStream(catalogSystemId))
                .orElse(null);

    this.catalogSystemId = catalogSystemId;

    load(in != null ? in : tryCatalog.get());
  }

  private static int category(final int c) {
    final IntSupplier tryWhite = () -> c == '\t' || c == '\n' || c == '\r' ? WHITE : OTHER;
    final IntSupplier trySpace = () -> c == ' ' ? SPACE : tryWhite.getAsInt();
    final IntSupplier tryDoubleQuote = () -> c == '\"' ? DOUBLE_QUOTE : trySpace.getAsInt();

    return c == '\'' ? SINGLE_QUOTE : tryDoubleQuote.getAsInt();
  }

  private static void error(final int in, final int line) throws IOException {
    if (in == EOF) {
      throw new IOException(linePrefix(line) + "premature end of file");
    }

    if (in == WHITE) {
      throw new IOException(linePrefix(line) + "\\t, \\n and \\r are not allowed in an identifier");
    }

    if (in == OTHER) {
      throw new IOException(linePrefix(line) + "white space expected");
    }
  }

  private static String getTypeToken(final char[] c, final int off, final int len, final int line)
      throws IOException {
    final StringTokenizer tokenizer = new StringTokenizer(new String(c, off, len), " \t\n\r");

    if (!tokenizer.hasMoreTokens()) {
      throw new IOException(linePrefix(line) + "PUBLIC or SYSTEM expected");
    }

    final String token = tokenizer.nextToken();

    if (!token.equals("PUBLIC") && !token.equals("SYSTEM")) {
      throw new IOException(linePrefix(line) + "PUBLIC or SYSTEM expected");
    }

    return token;
  }

  private static String linePrefix(final int line) {
    return "Line" + line + ": ";
  }

  /**
   * Returns a map from the public identifiers to the resolved URLs.
   *
   * @return The public identifier map.
   */
  public Map<String, String> getPublicIdentifierMappings() {
    return publicIdentifiers;
  }

  /**
   * Returns a map from the public identifiers to the resolved URLs.
   *
   * @return The system identifier map.
   */
  public Map<String, String> getSystemIdentifierMappings() {
    return systemIdentifiers;
  }

  private void load(final InputStream in) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    copy(in, out);

    final char[] c = out.toString(US_ASCII).toCharArray();
    String from = null;
    int line = 1;
    int position = 0;
    int state = TYP;
    String type = null;

    for (int i = 0; i < c.length; ++i) {
      final int[] next = FSM[state][category(c[i])];

      if (next[0] == ERR) {
        error(category(c[i]), line);
      }

      if (next[1] == 1) {
        final String from1 = new String(c, position, i - position);

        switch (state) {
          case TYP:
            type = getTypeToken(c, position, i - position, line);
            break;

          case SQ1, DQ1:
            from = from1;
            break;

          case SQ2, DQ2:
            ("PUBLIC".equals(type) ? publicIdentifiers : systemIdentifiers)
                .put(from, resolveSystemId(catalogSystemId, from1));

            break;

          default:
        }

        position = i + 1;
      }

      state = next[0];

      if (c[i] == '\n') {
        ++line;
      }
    }

    if (FSM[state][EOF][0] == ERR) {
      error(EOF, line);
    }
  }

  public InputSource resolveEntity(final String publicId, final String systemId) {
    final Supplier<String> trySystemId =
        () ->
            systemId != null && systemIdentifiers.get(systemId) != null
                ? systemIdentifiers.get(systemId)
                : systemId;
    final InputSource result =
        new InputSource(
            publicId != null && publicIdentifiers.get(publicId) != null
                ? publicIdentifiers.get(publicId)
                : trySystemId.get());

    result.setPublicId(publicId);

    return result;
  }

  public Object resolveEntity(
      final String publicId, final String systemId, final String baseURI, final String namespace) {
    final Supplier<String> tryBaseURI =
        () -> baseURI != null && systemId != null ? resolveSystemId(baseURI, systemId) : systemId;
    final Supplier<String> trySystemId =
        () ->
            systemId != null && systemIdentifiers.get(systemId) != null
                ? systemIdentifiers.get(systemId)
                : tryBaseURI.get();

    // This return type is not compliant with the StAX API, but there should
    // be a way to pass the resolved public ID, otherwise the base URI for
    // subsequent resolutions will be wrong.

    final StreamSource result =
        new StreamSource(
            publicId != null && publicIdentifiers.get(publicId) != null
                ? publicIdentifiers.get(publicId)
                : trySystemId.get());

    result.setPublicId(publicId);

    return result;
  }
}
