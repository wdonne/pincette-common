package net.pincette.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies an input stream to an output stream.
 *
 * @author Werner Donn\u00e9
 */
public class StreamConnector {
  private StreamConnector() {}

  /**
   * Closes <code>in</code> and <code>out</code> after copying.
   *
   * @param in
   * @param out
   * @throws IOException
   */
  public static void copy(final InputStream in, final OutputStream out) throws IOException {
    copy(in, out, true, true);
  }

  public static void copy(
      final InputStream in,
      final OutputStream out,
      final boolean closeInput,
      final boolean closeOutput)
      throws IOException {
    copy(in, out, 0x10000, closeInput, closeOutput);
  }

  public static void copy(
      final InputStream in,
      final OutputStream out,
      final boolean closeInput,
      final boolean closeOutput,
      final boolean flush)
      throws IOException {
    copy(in, out, 0x10000, closeInput, closeOutput, flush);
  }

  public static void copy(
      final InputStream in,
      final OutputStream out,
      final int bufferSize,
      final boolean closeInput,
      final boolean closeOutput)
      throws IOException {
    copy(in, out, bufferSize, closeInput, closeOutput, true);
  }

  public static void copy(
      final InputStream in,
      final OutputStream out,
      final int bufferSize,
      final boolean closeInput,
      final boolean closeOutput,
      final boolean flush)
      throws IOException {
    final byte[] buffer = new byte[bufferSize];
    int len;

    while ((len = in.read(buffer)) != -1) {
      out.write(buffer, 0, len);

      if (flush) {
        out.flush();
      }
    }

    if (closeInput) {
      in.close();
    }

    if (closeOutput) {
      out.close();
    } else {
      if (flush) {
        out.flush();
      }
    }
  }
}
