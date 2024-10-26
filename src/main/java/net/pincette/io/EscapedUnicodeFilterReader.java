package net.pincette.io;

import static java.lang.Character.toChars;
import static java.lang.Integer.parseInt;
import static java.lang.System.arraycopy;
import static net.pincette.util.Util.tryToGetSilent;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/**
 * This reader converts java-style Unicode escape codes to characters. It is a filter in order to
 * provide a combination with a physical encoding.
 *
 * @author Werner Donné
 */
public class EscapedUnicodeFilterReader extends FilterReader {
  private final char[] buffer = new char[6];
  private boolean end;
  private int position;
  private char[] readBuffer;
  private int readPosition;

  public EscapedUnicodeFilterReader(final Reader in) {
    super(new BufferedReader(in));
  }

  private static boolean isHex(final char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private void copyToReadBuffer() {
    if (position > 0) {
      readBuffer = new char[position];
      arraycopy(buffer, 0, readBuffer, 0, position);
    }

    readPosition = 0;
    position = 0;
  }

  private boolean inEscape() {
    return (position == 1 && buffer[position - 1] == '\\')
        || (position == 2 && buffer[position - 1] == 'u')
        || (position > 2 && position < 7 && isHex(buffer[position - 1]));
  }

  @Override
  public int read() throws IOException {
    final char[] b = new char[1];

    return read(b, 0, b.length) == -1 ? -1 : (0xffff & b[0]);
  }

  @Override
  public int read(final char[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(final char[] b, final int off, final int len) throws IOException {
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

  private int readOneCharacter() throws IOException {
    tryToEscape();

    if (readBuffer != null && readPosition < readBuffer.length) {
      return readBuffer[readPosition++];
    }

    if (end) {
      return -1;
    }

    final int c = in.read();

    if (c == -1) {
      if (position == 0) {
        end = true;
      } else {
        copyToReadBuffer();
      }

      return readOneCharacter();
    }

    buffer[position++] = (char) c;

    if (!inEscape()) {
      if (--position == 0) {
        return c;
      }

      copyToReadBuffer();
      buffer[0] = (char) c;
      position = 1;
    }

    return readOneCharacter();
  }

  private void tryToEscape() {
    if (position == 6) {
      Optional.of(buffer)
          .filter(b -> b[0] == '\\' && b[1] == 'u')
          .flatMap(
              b -> tryToGetSilent(() -> parseInt(new String(buffer, 2, buffer.length - 2), 16)))
          .flatMap(n -> tryToGetSilent(() -> toChars(n)))
          .ifPresentOrElse(
              c -> {
                readBuffer = c;
                readPosition = 0;
                position = 0;
              },
              this::copyToReadBuffer);
    }
  }
}
