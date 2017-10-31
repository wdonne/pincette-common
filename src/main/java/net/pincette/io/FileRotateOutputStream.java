package net.pincette.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;



/**
 * Rotates the given file based on the size in bytes and the depth.
 * @author Werner Donn\u00e9
 */

public class FileRotateOutputStream extends OutputStream

{

  private final int depth;
  private final long size;
  private final File file;
  private OutputStream out;



  public
  FileRotateOutputStream
  (
    final String filename,
    final int depth,
    final long size
  )
  {
    this(new File(filename), depth, size);
  }



  public
  FileRotateOutputStream(final File file, final int depth, final long size)
  {
    this.file = file;
    this.depth = depth;
    this.size = size;
  }



  public void
  close() throws IOException
  {
    if (out != null)
    {
      out.close();
    }
  }



  public void
  flush() throws IOException
  {
    if (out != null)
    {
      out.flush();
    }
  }



  private OutputStream
  rotate() throws IOException
  {
    if (out != null)
    {
      out.close();
    }

    for (int i = depth; i >= 1; --i)
    {
      final File log =
        new File(file.getAbsolutePath() + "." + String.valueOf(i) + ".gz");

      if (log.exists())
      {
        if (i == depth)
        {
          log.delete();
        }
        else
        {
          log.renameTo
          (
            new File
            (
              file.getAbsolutePath() + "." + String.valueOf(i + 1) + ".gz"
            )
          );
        }
      }
    }

    StreamConnector.copy
    (
      new FileInputStream(file),
      new GZIPOutputStream
      (
        new FileOutputStream(file.getAbsolutePath() + ".1.gz")
      )
    );

    file.delete();

    return new FileOutputStream(file);
  }



  public void
  write(final byte[] b) throws IOException
  {
    write(b, 0, b.length);
  }



  public void
  write(final byte[] b, final int off, final int len) throws IOException
  {
    out =
      !file.exists() || (out == null && file.length() + len <= size) ?
        new FileOutputStream(file, true) :
        (file.length() + len > size ? rotate() : out);
    out.write(b, off, len);
  }



  public void
  write(final int b) throws IOException
  {
    write(new byte[]{(byte) b}, 0, 1);
  }

} // FileRotateOutputStream
