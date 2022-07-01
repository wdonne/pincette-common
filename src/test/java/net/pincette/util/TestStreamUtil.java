package net.pincette.util;

import static java.util.stream.Collectors.toList;
import static net.pincette.util.Collections.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestStreamUtil {
  @Test
  @DisplayName("per")
  void per() {
    assertEquals(
        list(list(0, 1), list(2, 3)),
        StreamUtil.per(list(0, 1, 2, 3).stream(), 2).collect(toList()));
    assertEquals(
        list(list(0, 1, 2), list(3)),
        StreamUtil.per(list(0, 1, 2, 3).stream(), 3).collect(toList()));
    assertEquals(
        list(list(0, 1), list(2, 3), list(4)),
        StreamUtil.per(list(0, 1, 2, 3, 4).stream(), 2).collect(toList()));
    assertEquals(list(list(0)), StreamUtil.per(list(0).stream(), 2).collect(toList()));
    assertEquals(list(list(0), list(1)), StreamUtil.per(list(0, 1).stream(), 1).collect(toList()));
    assertEquals(list(), StreamUtil.per(list(0, 1).stream(), 0).collect(toList()));
    assertEquals(list(list(0, 1)), StreamUtil.per(list(0, 1).stream(), 10).collect(toList()));
  }

  @Test
  @DisplayName("slide")
  void slide() {
    assertEquals(
        list(list(0, 1), list(1, 2), list(2, 3)),
        StreamUtil.slide(list(0, 1, 2, 3).stream(), 2).collect(toList()));
    assertEquals(
        list(list(0, 1, 2), list(1, 2, 3)),
        StreamUtil.slide(list(0, 1, 2, 3).stream(), 3).collect(toList()));
    assertEquals(list(), StreamUtil.slide(list(0).stream(), 2).collect(toList()));
    assertEquals(list(list(0, 1)), StreamUtil.slide(list(0, 1).stream(), 2).collect(toList()));
  }
}
