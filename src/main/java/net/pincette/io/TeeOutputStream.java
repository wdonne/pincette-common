package net.pincette.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Replicates the output to several OutputStreams in the order given in the constructor.
 *
 * @author Werner Donn\u00e9
 */
public class TeeOutputStream extends OutputStream {
  private final OutputStream[] out;

  public TeeOutputStream(final OutputStream[] out) {
    this.out = out;
  }

  @Override
  public void close() throws IOException {
    for (OutputStream o : out) {
      o.close();
    }
  }

  @Override
  public void flush() throws IOException {
    for (OutputStream o : out) {
      o.flush();
    }
  }

  public OutputStream[] getOutputStreams() {
    return out;
  }

  public void write(final int b) throws IOException {
    for (OutputStream o : out) {
      o.write(b);
    }
  }

  @Override
  public void write(final byte[] b) throws IOException {
    for (OutputStream o : out) {
      o.write(b);
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    for (OutputStream o : out) {
      o.write(b, off, len);
    }
  }
}
