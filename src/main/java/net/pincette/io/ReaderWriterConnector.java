package net.pincette.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * This class reads data from reader and writes it to writer in a separate thread. The thread stops
 * when there is no more input or when an exception occurs.
 *
 * @author Werner Donn\u00e9
 */
public class ReaderWriterConnector {
  private ReaderWriterConnector() {}

  /**
   * Closes <code>in</code> and <code>out</code> after copying.
   *
   * @param in the consumed reader.
   * @param out the writer to which the data is written.
   * @throws IOException when something goes wrong.
   */
  public static void copy(final Reader in, final Writer out) throws IOException {
    copy(in, out, true, true);
  }

  public static void copy(
      final Reader in, final Writer out, final boolean closeInput, final boolean closeOutput)
      throws IOException {
    copy(in, out, 0x10000, closeInput, closeOutput);
  }

  public static void copy(
      final Reader in,
      final Writer out,
      final int bufferSize,
      final boolean closeInput,
      final boolean closeOutput)
      throws IOException {
    char[] buffer = new char[bufferSize];
    int len;

    while ((len = in.read(buffer)) != -1) {
      out.write(buffer, 0, len);
      out.flush();
    }

    if (closeInput) {
      in.close();
    }

    if (closeOutput) {
      out.close();
    } else {
      out.flush();
    }
  }
}
