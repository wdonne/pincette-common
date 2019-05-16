package net.pincette.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FlushOutputStream extends FilterOutputStream {
  public FlushOutputStream(final OutputStream out) {
    super(out);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    out.write(b, off, len);
    out.flush();
  }

  @Override
  public void write(final int b) throws IOException {
    out.write(b);
    out.flush();
  }
}
