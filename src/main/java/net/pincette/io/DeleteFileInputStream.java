package net.pincette.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;



/**
 * Deletes the file after the stream has been closed, which is interesting when
 * working with temporary files.
 * @author Werner Donn\u00e9
 */

public class DeleteFileInputStream extends FileInputStream

{

  final private File file;



  public
  DeleteFileInputStream(final File file) throws FileNotFoundException
  {
    super(file);
    this.file = file;
  }



  public void
  close() throws IOException
  {
    super.close();
    file.delete();
  }

} // DeleteFileInputStream
