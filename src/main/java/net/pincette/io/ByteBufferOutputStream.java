package net.pincette.io;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Writes the bytes to a series of buffers. It will request as many buffers as needed.
 *
 * @since 2.2
 * @author Werner Donné
 */
public class ByteBufferOutputStream extends OutputStream {
  private final Supplier<ByteBuffer> get;
  private final Runnable onFlush;

  public ByteBufferOutputStream(final Supplier<ByteBuffer> get) {
    this(get, null);
  }

  public ByteBufferOutputStream(final Supplier<ByteBuffer> get, final Runnable onFlush) {
    this.get = get;
    this.onFlush = onFlush;
  }

  @Override
  public void close() {
    flush();
  }

  @Override
  public void flush() {
    if (onFlush != null) {
      onFlush.run();
    }
  }

  @Override
  public void write(final int b) throws IOException {

    write(new byte[] {(byte) b}, 0, 1);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    if (len > 0) {
      final ByteBuffer buffer = get.get();
      final int size = min(len, buffer.remaining());

      buffer.put(b, off, size);

      if (size < len) {
        write(b, off + size, len - size);
      }
    }
  }
}
