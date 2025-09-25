package net.pincette.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * See also RFC 2045 section 6.8.
 *
 * @author Werner Donné
 */
public class Base64InputStream extends FilterInputStream {
  private static final byte[] values = createValues();
  private final byte[] decodedBuffer = new byte[3];
  // The room for one decoded quantum.
  private byte[] encodedBuffer = null;
  private int encodedPosition = 0;
  private int length = 0;
  private int quantumPosition = 0;

  public Base64InputStream(final InputStream in) {
    super(in);
  }

  private static byte[] createValues() {
    byte j = 0;
    byte[] result = new byte[256];

    Arrays.fill(result, (byte) -1);

    for (int i = 'A'; i <= 'Z'; ++i, ++j) {
      result[i] = j;
    }

    for (int i = 'a'; i <= 'z'; ++i, ++j) {
      result[i] = j;
    }

    for (int i = '0'; i <= '9'; ++i, ++j) {
      result[i] = j;
    }

    result['+'] = j++;
    result['/'] = j;

    return result;
  }

  @Override
  public int available() {
    return 0;
    // This can't be predicted because non-alphabet bytes must be ignored and
    // we can't know up front how many there will be of them.
  }

  private void checkQuantumPosition() {
    if (++quantumPosition == 4) {
      Arrays.fill(decodedBuffer, (byte) 0);
      quantumPosition = 0;
    }
  }

  private int decodeQuantumPosition(final byte[] b, final int position, final int off) {
    return switch (quantumPosition) {
      case 0 -> decode0(position);
      case 1 -> decode1(b, position, off);
      case 2 -> decode2(b, position, off);
      case 3 -> decode3(b, position, off);
      default -> position;
    };
  }

  private int decode0(final int position) {
    decodedBuffer[0] = (byte) (values[encodedBuffer[encodedPosition]] << 2);

    return position;
  }

  private int decode1(final byte[] b, final int position, final int off) {
    decodedBuffer[0] |= (byte) ((values[encodedBuffer[encodedPosition]] >> 4) & 0x03);
    decodedBuffer[1] = (byte) (values[encodedBuffer[encodedPosition]] << 4);
    b[position + off] = decodedBuffer[0];

    return position + 1;
  }

  private int decode2(final byte[] b, final int position, final int off) {
    if (encodedBuffer[encodedPosition] != '=') {
      decodedBuffer[1] |= (byte) ((values[encodedBuffer[encodedPosition]] >> 2) & 0x0f);
      decodedBuffer[2] = (byte) (values[encodedBuffer[encodedPosition]] << 6);
      b[position + off] = decodedBuffer[1];

      return position + 1;
    }

    return position;
  }

  private int decode3(final byte[] b, final int position, final int off) {
    if (encodedBuffer[encodedPosition] != '=') {
      decodedBuffer[2] |= values[encodedBuffer[encodedPosition]];
      b[position + off] = decodedBuffer[2];

      return position + 1;
    }

    return position;
  }

  private boolean prepareBuffer(final int len) throws IOException {
    if (length == -1) {
      return false;
    }

    if (encodedBuffer == null || encodedPosition == length) {
      encodedBuffer = new byte[Math.max(2 * len, 4)];
      // Four input bytes become three bytes, so this is large enough to hold
      // enough input for decoding.
      encodedPosition = 0;
      length = in.read(encodedBuffer);

      return length != -1;
    }

    return true;
  }

  @Override
  public int read() throws IOException {
    final byte[] b = new byte[1];

    return read(b, 0, b.length) == -1 ? -1 : (255 & b[0]);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (!prepareBuffer(len)) {
      return -1;
    }

    int i = 0;

    for (; encodedPosition < length && i < len; ++encodedPosition) {
      if (values[encodedBuffer[encodedPosition]] != -1 || encodedBuffer[encodedPosition] == '=') {
        i = decodeQuantumPosition(b, i, off);
        checkQuantumPosition();
      }
    }

    return i < len ? readAdditional(b, i, off, len) : len;
  }

  private int readAdditional(final byte[] b, int position, final int off, final int len)
      throws IOException {
    final int initial = position == 0 ? -1 : position;
    final int bytesRead = read(b, off + position, len - position);

    return bytesRead == -1 ? initial : position + bytesRead;
  }

  @Override
  public long skip(final long n) {
    return 0; // See available method.
  }
}
