package net.pincette.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps another output stream and protects it against being closed.
 *
 * @author Werner Donn\u00e9
 */
@SuppressWarnings("squid:S4349")
public class DontCloseOutputStream extends FilterOutputStream {

  public DontCloseOutputStream(final OutputStream in) {
    super(in);
  }

  @Override
  public void close() throws IOException {
    flush();
  }
}
