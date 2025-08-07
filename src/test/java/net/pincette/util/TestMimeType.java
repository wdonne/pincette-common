package net.pincette.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestMimeType {
  private static final String TYPE = "text/plain";

  @Test
  @DisplayName("getContentTypeFromName")
  void getContentTypeFromName() {
    assertEquals(TYPE, MimeType.getContentTypeFromName("file.txt"));
    assertEquals(TYPE, MimeType.getContentTypeFromName("file.TXT"));
    assertEquals(TYPE, MimeType.getContentTypeFromName("file.hh"));
    assertEquals(TYPE, MimeType.getContentTypeFromName("file.LOG"));
  }
}
