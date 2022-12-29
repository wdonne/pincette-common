package net.pincette.io;

import static java.lang.Math.min;

import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

/**
 * Creates an input stream from a list of buffers.
 *
 * @author Werner Donn\u00e9
 * @since 2.2
 */
public class ByteBufferInputStream extends InputStream {
  private final List<ByteBuffer> buffers;

  public ByteBufferInputStream(final List<ByteBuffer> buffers) {
    this.buffers = buffers;
  }

  private Optional<ByteBuffer> findRemaining() {
    return buffers.stream().filter(Buffer::hasRemaining).findFirst();
  }

  @Override
  public int read() {
    final byte[] b = new byte[1];

    return read(b, 0, b.length) == -1 ? -1 : (255 & b[0]);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) {
    return len > 0
        ? findRemaining()
            .map(
                buffer -> {
                  final int size = min(len, buffer.remaining());

                  buffer.get(b, off, size);

                  return size;
                })
            .orElse(-1)
        : 0;
  }
}
