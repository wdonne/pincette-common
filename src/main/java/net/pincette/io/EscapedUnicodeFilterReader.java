package net.pincette.io;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.function.UnaryOperator;

/**
 * This reader converts java-style Unicode escape codes to characters. It is a
 * filter in order to provide a combination with a physical encoding.
 *
 * @author Werner Donn\u00e9
 */

public class EscapedUnicodeFilterReader extends FilterReader

{

  // Alphabet.

  private static final int BACK_SLASH = 0;
  private static final int U = 1;
  private static final int HEX = 2;
  private static final int OTHER = 3;

  // States.

  private static final int START = 0;
  private static final int U1 = 1;
  private static final int U2 = 2;
  private static final int H1 = 3;
  private static final int H2 = 4;
  private static final int H3 = 5;
  private static final int ACCEPT = 6;

  private static final int[][] FSM = {
      {U1, START, START, START}, // START
      {START, U2, START, START}, // U1
      {START, START, H1, START}, // U2
      {START, START, H2, START}, // H1
      {START, START, H3, START}, // H2
      {START, START, ACCEPT, START} // H3
  };

  private boolean end = false;

  public EscapedUnicodeFilterReader(final Reader in) throws IOException {
    super(new PushbackReader(new BufferedReader(in), 1024));
  }

  private static int
  category(final int c) {
    final UnaryOperator<Integer> hexOrOther =
        ch ->
            (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F') ?
                HEX : OTHER;
    final UnaryOperator<Integer> uOr = ch -> c == 'u' ? U : hexOrOther.apply(ch);

    return c == '\\' ? BACK_SLASH : uOr.apply(c);
  }

  @Override
  public int
  read() throws IOException {
    final char[] b = new char[1];

    return read(b, 0, b.length) == -1 ? -1 : (0xffff & b[0]);
  }

  @Override
  public int
  read(final char[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int
  read(final char[] b, final int off, final int len) throws IOException {
    if (end) {
      return -1;
    }

    int i = 0;

    for (; i < len && !end; ++i) {
      final int c = readOneCharacter();

      if (c != -1) {
        b[off + i] = (char) c;
      } else {
        end = true;
        --i;
      }
    }

    return i == 0 && end ? -1 : i;
  }

  private int
  readOneCharacter() throws IOException {
    final StringBuilder buffer = new StringBuilder();
    int c;
    int state = START;

    for
        (
        c = in.read();
        c != -1 && (state = FSM[state][category(c)]) != START && state != ACCEPT;
        c = in.read()
        ) {
      buffer.append(c);
    }

    if (state == ACCEPT) {
      return
          Integer.
              parseInt(buffer.substring(2) + new String(new char[]{(char) c}), 16);
    }

    if (buffer.length() == 0) {
      return c;
    }

    ((PushbackReader) in).unread(c);

    if (buffer.length() > 1) {
      ((PushbackReader) in).unread(buffer.substring(1).toCharArray());
    }

    return buffer.length() > 0 ? buffer.charAt(0) : -1;
  }

} // EscapedUnicodeFilterReader
