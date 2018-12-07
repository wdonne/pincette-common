package net.pincette.io;

import java.io.InputStream;

/** An empty input stream. */
public class DevNullInputStream extends InputStream {
  @Override
  public int available() {
    return 0;
  }

  public int read() {
    return -1;
  }

  @Override
  public int read(final byte[] b) {
    return -1;
  }

  @Override
  public int read(final byte[] b, final int off, final int len) {
    return -1;
  }
}
