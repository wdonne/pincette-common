package net.pincette.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wraps another input stream and protects it against being closed.
 *
 * @author Werner Donn\u00e9
 */
public class DontCloseInputStream extends FilterInputStream {

  public DontCloseInputStream(final InputStream in) {
    super(in);
  }

  @Override
  public void close() throws IOException {
    // Purpose of class.
  }
}
