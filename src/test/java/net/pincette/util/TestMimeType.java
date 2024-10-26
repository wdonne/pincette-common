package net.pincette.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestMimeType {
  @Test
  @DisplayName("getContentTypeFromName")
  void getContentTypeFromName() {
    assertEquals("text/plain", MimeType.getContentTypeFromName("file.txt"));
    assertEquals("text/plain", MimeType.getContentTypeFromName("file.TXT"));
    assertEquals("text/plain", MimeType.getContentTypeFromName("file.hh"));
    assertEquals("text/plain", MimeType.getContentTypeFromName("file.LOG"));
  }
}
