package net.pincette.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;



/**
 * Wraps another output stream and protects it against being closed.
 * @author Werner Donn\u00e9
 */

public class DontCloseOutputStream extends FilterOutputStream

{

  public
  DontCloseOutputStream(final OutputStream in)
  {
    super(in);
  }



  public void
  close() throws IOException
  {
    flush();
  }

} // DontCloseOutputStream
