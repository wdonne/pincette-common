package net.pincette.cls;

import java.io.IOException;

public class NotAClassException extends IOException {

  public NotAClassException() {
    super("Not a class");
  }
}
