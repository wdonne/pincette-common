package net.pincette.io;

import static java.nio.file.Files.delete;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Deletes the file after the stream has been closed, which is interesting when working with
 * temporary files.
 *
 * @author Werner Donn\u00e9
 */
public class DeleteFileInputStream extends FileInputStream {
  private final File file;

  public DeleteFileInputStream(final File file) throws FileNotFoundException {
    super(file);
    this.file = file;
  }

  @Override
  public void close() throws IOException {
    super.close();
    delete(file.toPath());
  }
}
