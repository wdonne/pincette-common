package net.pincette.io;

import java.io.OutputStream;

/**
 * This output stream gobbles all output.
 *
 * @author Werner Donné
 */
public class DevNullOutputStream extends OutputStream {
  @Override
  public void close() {
    // Purpose of the class.
  }

  @Override
  public void flush() {
    // Purpose of the class.
  }

  @Override
  public void write(final byte[] b) {
    // Purpose of the class.
  }

  @Override
  public void write(final byte[] b, final int off, final int len) {
    // Purpose of the class.
  }

  public void write(final int b) {
    // Purpose of the class.
  }
}
