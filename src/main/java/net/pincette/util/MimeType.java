package net.pincette.util;

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static net.pincette.util.Array.hasPrefix;
import static net.pincette.util.Collections.put;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.rangeExclusive;
import static net.pincette.util.Util.readLineConfig;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.xml.Util.isXml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Some MIME type utilities.
 *
 * @author Werner Donné
 */
public class MimeType {
  private static final String OCTET_STREAM = "application/octet-stream";

  private static final Map<String, String> extensions;
  private static String[] knownMimeTypes;
  private static final Map<String, Set<String>> mimeTypes;
  private static Map<String, String> preferredMimeTypes;

  static {
    final Pair<Map<String, Set<String>>, Map<String, String>> result = loadMimeTypeMap();

    mimeTypes = result.first;
    extensions = result.second;
  }

  private MimeType() {}

  public static String adjustExtension(final String filename, final String mimeType) {
    final int index = filename.lastIndexOf('.');

    if (index != -1
        && mimeType.equals(getContentTypeFromExtension(filename.substring(index + 1)))) {
      return filename;
    }

    final String[] extensions = getExtensionsFromMimeType(mimeType);

    if (extensions.length == 0) {
      return filename;
    }

    return (index == -1 ? filename : filename.substring(0, index)) + "." + extensions[0];
  }

  public static String anySubtype(final String mimeType) {
    return Optional.of(mimeType.indexOf('/'))
        .filter(index -> index != -1)
        .map(index -> mimeType.substring(0, index) + "/*")
        .orElse(mimeType);
  }

  public static String anyXMLSubtype(final String mimeType) {
    return Optional.of(mimeType.indexOf('/'))
        .filter(index -> index != -1)
        .map(index -> mimeType.substring(0, index) + "/*+xml")
        .orElse(mimeType);
  }

  /**
   * Returns the MIME type in lower case and the parameters in alphabetical order, which facilitates
   * comparison.
   *
   * @param mimeType the original MIME type.
   * @return The transformed MIME type.
   */
  public static String canonical(final String mimeType) {
    return getParameters(mimeType).entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .sorted()
        .reduce(
            new StringBuilder(stripParameters(mimeType).toLowerCase()),
            (b, p) -> b.append(';').append(p),
            (b1, b2) -> b1)
        .toString();
  }

  /**
   * Tries to detect the MIME type through magic numbers. If the type is not detected
   * application/octet-stream is returned.
   *
   * @param b the given byte stream.
   * @return The detected MIME type.
   */
  public static String detectType(final byte[] b) {
    final Supplier<String> pngOr =
        () ->
            b.length > 7
                    && hasPrefix(
                        b, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})
                ? "image/png"
                : OCTET_STREAM;
    final Supplier<String> jpegOr =
        () ->
            b.length > 3
                    && b[0] == (byte) 0xff
                    && b[1] == (byte) 0xd8
                    && b[b.length - 2] == (byte) 0xff
                    && b[b.length - 1] == (byte) 0xd9
                ? "image/jpeg"
                : pngOr.get();

    return b.length > 5 && hasPrefix(b, new byte[] {0x47, 0x49, 0x46, 0x38, 0x39, 0x61})
        ? "image/gif"
        : jpegOr.get();
  }

  /**
   * The default is application/octet-stream.
   *
   * @param extension the given extension.
   * @return The mapped MIME type.
   */
  public static String getContentTypeFromExtension(final String extension) {
    return ofNullable(extensions.get(extension.toLowerCase())).orElse(OCTET_STREAM);
  }

  /**
   * Return the MIME type for a name. The default is application/octet-stream.
   *
   * @param name the given name, e.g. a filename, URI, etc.
   * @return The mapped MIME type.
   */
  public static String getContentTypeFromName(final String name) {
    return Optional.of(name.lastIndexOf('.'))
        .filter(index -> index != -1)
        .map(index -> getContentTypeFromExtension(name.substring(index + 1)))
        .orElse(OCTET_STREAM);
  }

  public static String[] getExtensionsFromMimeType(final String mimeType) {
    return ofNullable(mimeTypes.get(stripParameters(mimeType).toLowerCase()))
        .map(set -> set.toArray(new String[0]))
        .orElse(new String[0]);
  }

  public static String[] getKnownMimeTypes() {
    if (knownMimeTypes == null) {
      knownMimeTypes = loadKnownMimeTypes();
    }

    return knownMimeTypes;
  }

  /**
   * Returns the media type part in lower case.
   *
   * @param mimeType teh given MIME type.
   * @return The media type.
   */
  public static String getMediaType(final String mimeType) {
    return Optional.of(mimeType.indexOf('/'))
        .filter(index -> index != -1)
        .map(index -> mimeType.substring(0, index).toLowerCase())
        .orElseGet(mimeType::toLowerCase);
  }

  public static Optional<String> getParameter(final String mimeType, final String name) {
    return ofNullable(getParameters(mimeType).get(name.toLowerCase()));
  }

  private static Optional<String> getParameterValue(final String value) {
    return Optional.of(value.split("[ \"']"))
        .filter(tokens -> tokens.length == 1)
        .map(tokens -> tokens[0]);
  }

  /**
   * The parameter names in lower case are the keys. The values are copied literally.
   *
   * @param mimeType the given MIME type.
   * @return The MIME type parameters.
   */
  public static Map<String, String> getParameters(final String mimeType) {
    return stream(mimeType.split(";"))
        .map(String::trim)
        .map(s -> pair(s, s.indexOf('=')))
        .filter(pair -> pair.second != -1)
        .map(
            pair ->
                pair(
                    pair.first.substring(0, pair.second).trim().toLowerCase(),
                    getParameterValue(pair.first.substring(pair.second + 1))))
        .filter(pair -> pair.second.isPresent())
        .collect(toMap(pair -> pair.first, pair -> pair.second.orElse(null)));
  }

  /**
   * Sometimes more than one MIME type is used for the same thing. This method returns the preferred
   * MIME type in lower case.
   *
   * @param mimeType the given MIME type.
   * @return The preferred substitute.
   */
  public static String getPreferred(final String mimeType) {
    if (preferredMimeTypes == null) {
      preferredMimeTypes = loadPreferredMimeTypes();
    }

    return ofNullable(preferredMimeTypes.get(mimeType.toLowerCase()))
        .orElseGet(mimeType::toLowerCase);
  }

  /**
   * Returns the subtype in lower case or * if there is no subtype.
   *
   * @param mimeType the given MIME type.
   * @return The substype.
   */
  public static String getSubtype(final String mimeType) {
    return Optional.of(mimeType.indexOf('/'))
        .filter(index -> index != -1)
        .map(index -> stripParameters(mimeType.substring(index + 1)).toLowerCase())
        .orElse("*");
  }

  public static boolean isGenericXML(final String type) {
    return "*/*".equals(type)
        || "*/*+xml".equals(type)
        || "text/xml".equals(type)
        || "application/xml".equals(type)
        || "application/*+xml".equals(type);
  }

  public static boolean isSubtype(final String type, final String subtype) {
    return "*/*".equals(type) || anySubtype(subtype).equals(type);
  }

  public static boolean isXMLSubtype(final String type, final String subtype) {
    return isXml(subtype) && isGenericXML(type) && !isGenericXML(subtype);
  }

  public static String[] loadKnownMimeTypes() {
    return tryToGetRethrow(
            () ->
                readLineConfig(
                        new BufferedReader(
                            new InputStreamReader(
                                Objects.requireNonNull(
                                    MimeType.class.getResourceAsStream("res/mime_types")))))
                    .map(String::toLowerCase)
                    .toArray(String[]::new))
        .orElse(new String[0]);
  }

  private static Pair<Map<String, Set<String>>, Map<String, String>> loadMimeTypeMap() {
    final Pair<Map<String, Set<String>>, Map<String, String>> result =
        pair(new HashMap<>(), new HashMap<>());

    tryToDoRethrow(
        () ->
            parse(
                MimeType.class.getResourceAsStream("res/mime_types.map"),
                result.first,
                result.second));

    return result;
  }

  private static Map<String, String> loadPreferredMimeTypes() {
    final Properties properties = new Properties();

    tryToDoRethrow(
        () -> properties.load(MimeType.class.getResourceAsStream("res/preferred_mime_types.map")));

    return properties.entrySet().stream()
        .collect(
            toMap(
                e -> e.getKey().toString().toLowerCase(),
                e -> e.getValue().toString().toLowerCase()));
  }

  private static void mapExtensionsToType(
      final Map<String, String> extensions, final String[] tokens) {
    rangeExclusive(1, tokens.length).forEach(i -> extensions.put(tokens[i], tokens[0]));
  }

  private static void mapTypeToExtensions(
      final Map<String, Set<String>> mimeTypes, final String[] tokens) {
    mimeTypes.put(tokens[0], set(copyOfRange(tokens, 1, tokens.length)));
  }

  private static void parse(
      final InputStream in,
      final Map<String, Set<String>> mimeTypes,
      final Map<String, String> extensions) {
    readLineConfig(in)
        .map(line -> line.split("[ \t]"))
        .map(
            tokens ->
                stream(tokens)
                    .filter(t -> !t.isEmpty())
                    .map(String::toLowerCase)
                    .toArray(String[]::new))
        .filter(tokens -> tokens.length > 1)
        .forEach(
            tokens -> {
              mapExtensionsToType(extensions, tokens);
              mapTypeToExtensions(mimeTypes, tokens);
            });
  }

  public static String setParameter(final String mimeType, final String name, final String value) {
    return stripParameters(mimeType)
        + put(getParameters(mimeType), name.toLowerCase(), value).entrySet().stream()
            .map(e -> ";" + e.getKey() + "=\"" + e.getValue() + "\"")
            .collect(joining());
  }

  /**
   * Returns the MIME type without its parameters if it has any.
   *
   * @param mimeType the given MIME type.
   * @return The MIME type without parameters.
   */
  public static String stripParameters(final String mimeType) {
    return Optional.of(mimeType.indexOf(';'))
        .filter(index -> index != -1)
        .map(index -> mimeType.substring(0, index))
        .orElse(mimeType);
  }
}
