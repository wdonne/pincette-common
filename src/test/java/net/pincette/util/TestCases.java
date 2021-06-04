package net.pincette.util;

import static net.pincette.util.Cases.withValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestCases {
  @Test
  @DisplayName("Cases")
  void test() {
    assertEquals(0, withValue(0).or(v -> true, v -> 0).or(v -> false, v -> 1).get().orElse(-1));
    assertEquals(1, withValue(0).or(v -> false, v -> 0).or(v -> true, v -> 1).get().orElse(-1));
    assertEquals(-1, withValue(0).or(v -> false, v -> 0).or(v -> false, v -> 1).get().orElse(-1));
  }
}
