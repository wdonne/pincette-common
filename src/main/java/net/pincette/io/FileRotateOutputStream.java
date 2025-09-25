package net.pincette.io;

import static java.nio.file.Files.delete;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Util.tryToDoWithRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

/**
 * Rotates the given file based on the size in bytes and the depth.
 *
 * @author Werner Donné
 */
public class FileRotateOutputStream extends OutputStream {

  private final int depth;
  private final long size;
  private final File file;
  private OutputStream out;

  public FileRotateOutputStream(final String filename, final int depth, final long size) {
    this(new File(filename), depth, size);
  }

  public FileRotateOutputStream(final File file, final int depth, final long size) {
    this.file = file;
    this.depth = depth;
    this.size = size;
  }

  @Override
  public void close() throws IOException {
    if (out != null) {
      out.close();
    }
  }

  @Override
  public void flush() throws IOException {
    if (out != null) {
      out.flush();
    }
  }

  private OutputStream rotate() throws IOException {
    if (out != null) {
      out.close();
    }

    for (int i = depth; i >= 1; --i) {
      final File log = new File(file.getAbsolutePath() + "." + i + ".gz");

      if (log.exists()) {
        if (i == depth) {
          delete(log.toPath());
        } else if (!log.renameTo(new File(file.getAbsolutePath() + "." + (i + 1) + ".gz"))) {
          throw new IOException("Rename failed");
        }
      }
    }

    tryToDoWithRethrow(
        () -> new FileInputStream(file),
        in ->
            copy(in, new GZIPOutputStream(new FileOutputStream(file.getAbsolutePath() + ".1.gz"))));

    delete(file.toPath());

    return new FileOutputStream(file);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    final Supplier<OutputStream> ifExceeded =
        () -> file.length() + len > size ? tryToGetRethrow(this::rotate).orElse(null) : out;

    out =
        !file.exists() || (out == null && file.length() + len <= size)
            ? new FileOutputStream(file, true)
            : ifExceeded.get();

    if (out != null) {
      out.write(b, off, len);
    }
  }

  public void write(final int b) throws IOException {
    write(new byte[] {(byte) b}, 0, 1);
  }
}
