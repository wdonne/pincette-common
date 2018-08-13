package net.pincette.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FlushOutputStream extends FilterOutputStream {
  public FlushOutputStream(final OutputStream out) {
    super(out);
  }

  public void write(final byte b) throws IOException {
    out.write(b);
    out.flush();
  }
}
