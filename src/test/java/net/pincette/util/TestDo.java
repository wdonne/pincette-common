package net.pincette.util;

import static java.util.Optional.empty;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Do.withValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestDo {
  @Test
  @DisplayName("Do")
  void test() {
    final List<Integer> results = new ArrayList<>();

    withValue(0).or(v -> v >= 0, results::add).or(v -> false, v -> {});
    withValue(1).or(v -> v == 0, v -> {}).or(v -> v > 0, results::add);
    withValue(2).or(v -> v < 2, results::add).or(v -> v == 0, results::add);
    withValue(0).orGet(v -> Optional.of(0), results::add);
    withValue(1).orGet(v -> empty(), v -> {});

    assertEquals(list(0, 1, 0), results);
  }
}
