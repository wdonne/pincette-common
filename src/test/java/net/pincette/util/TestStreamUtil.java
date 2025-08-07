package net.pincette.util;

import static net.pincette.util.Collections.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestStreamUtil {
  @Test
  @DisplayName("per")
  void per() {
    assertEquals(
        list(list(0, 1), list(2, 3)), StreamUtil.per(list(0, 1, 2, 3).stream(), 2).toList());
    assertEquals(
        list(list(0, 1, 2), list(3)), StreamUtil.per(list(0, 1, 2, 3).stream(), 3).toList());
    assertEquals(
        list(list(0, 1), list(2, 3), list(4)),
        StreamUtil.per(list(0, 1, 2, 3, 4).stream(), 2).toList());
    assertEquals(list(list(0)), StreamUtil.per(list(0).stream(), 2).toList());
    assertEquals(list(list(0), list(1)), StreamUtil.per(list(0, 1).stream(), 1).toList());
    assertEquals(list(), StreamUtil.per(list(0, 1).stream(), 0).toList());
    assertEquals(list(list(0, 1)), StreamUtil.per(list(0, 1).stream(), 10).toList());
  }

  @Test
  @DisplayName("slide")
  void slide() {
    assertEquals(
        list(list(0, 1), list(1, 2), list(2, 3)),
        StreamUtil.slide(list(0, 1, 2, 3).stream(), 2).toList());
    assertEquals(
        list(list(0, 1, 2), list(1, 2, 3)),
        StreamUtil.slide(list(0, 1, 2, 3).stream(), 3).toList());
    assertEquals(list(), StreamUtil.slide(list(0).stream(), 2).toList());
    assertEquals(list(list(0, 1)), StreamUtil.slide(list(0, 1).stream(), 2).toList());
  }

  @Test
  @DisplayName("tail")
  void tail() {
    assertEquals(list(1, 2), StreamUtil.tail(list(0, 1, 2).stream()).toList());
    assertEquals(list(), StreamUtil.tail(list(0).stream()).toList());
    assertEquals(list(), StreamUtil.tail(list().stream()).toList());
  }
}
