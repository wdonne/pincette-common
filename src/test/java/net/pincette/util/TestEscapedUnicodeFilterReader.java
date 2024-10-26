package net.pincette.util;

import static net.pincette.io.ReaderWriterConnector.copy;
import static net.pincette.util.Util.tryToDoRethrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import net.pincette.io.EscapedUnicodeFilterReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestEscapedUnicodeFilterReader {
  private static String test(final String s) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(
        () ->
            copy(
                new EscapedUnicodeFilterReader((new StringReader(s))),
                new OutputStreamWriter(out)));

    return out.toString();
  }

  @Test
  @DisplayName("EscapedUnicodeFilterReader")
  void testEscapedUnicodeFilterReader() {
    assertEquals("abc\\", test("a\\u0062c\\"));
    assertEquals("abc", test("\\u0061b\\u0063"));
    assertEquals("\\a", test("\\a"));
    assertEquals("\\ua", test("\\ua"));
    assertEquals("\\u000z", test("\\u000z"));
  }
}
