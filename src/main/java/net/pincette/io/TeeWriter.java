package net.pincette.io;

import java.io.IOException;
import java.io.Writer;

/**
 * Replicates the output to several Writers in the order given in the constructor.
 *
 * @author Werner Donn\u00e9
 */
public class TeeWriter extends Writer {
  private final Writer[] writers;

  public TeeWriter(final Writer[] writers) {
    this.writers = writers;
  }

  @Override
  public Writer append(final char c) throws IOException {
    for (Writer w : writers) {
      w.append(c);
    }

    return this;
  }

  @Override
  public Writer append(final CharSequence csq) throws IOException {
    for (Writer w : writers) {
      w.append(csq);
    }

    return this;
  }

  @Override
  public Writer append(final CharSequence csq, final int start, final int end) throws IOException {
    for (Writer w : writers) {
      w.append(csq, start, end);
    }

    return this;
  }

  public void close() throws IOException {
    for (Writer w : writers) {
      w.close();
    }
  }

  public void flush() throws IOException {
    for (Writer w : writers) {
      w.flush();
    }
  }

  @Override
  public void write(final char[] buf) throws IOException {
    for (Writer w : writers) {
      w.write(buf);
    }
  }

  public void write(final char[] buf, final int off, final int len) throws IOException {
    for (Writer w : writers) {
      w.write(buf, off, len);
    }
  }

  @Override
  public void write(final int c) throws IOException {
    for (Writer w : writers) {
      w.write(c);
    }
  }

  @Override
  public void write(final String str) throws IOException {
    for (Writer w : writers) {
      w.write(str);
    }
  }

  @Override
  public void write(final String str, final int off, final int len) throws IOException {
    for (Writer w : writers) {
      w.write(str, off, len);
    }
  }
}
