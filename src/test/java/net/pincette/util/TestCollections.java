package net.pincette.util;

import static java.util.stream.Collectors.toList;
import static net.pincette.util.Collections.computeIfAbsent;
import static net.pincette.util.Collections.computeIfPresent;
import static net.pincette.util.Collections.concat;
import static net.pincette.util.Collections.difference;
import static net.pincette.util.Collections.expand;
import static net.pincette.util.Collections.flatten;
import static net.pincette.util.Collections.intersection;
import static net.pincette.util.Collections.list;
import static net.pincette.util.Collections.map;
import static net.pincette.util.Collections.merge;
import static net.pincette.util.Collections.multiply;
import static net.pincette.util.Collections.put;
import static net.pincette.util.Collections.remove;
import static net.pincette.util.Collections.reverse;
import static net.pincette.util.Collections.set;
import static net.pincette.util.Collections.shiftDown;
import static net.pincette.util.Collections.shiftUp;
import static net.pincette.util.Collections.union;
import static net.pincette.util.Pair.pair;
import static net.pincette.util.StreamUtil.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestCollections {
  @Test
  @DisplayName("computeIfAbsent")
  void testComputeIfAbsent() {
    final Map<String, Integer> map = map(pair("0", 0));

    assertEquals(0, computeIfAbsent(new HashMap<>(map), "0", k -> 0));
    assertEquals(1, computeIfAbsent(new HashMap<>(map), "1", k -> 1));
  }

  @Test
  @DisplayName("computeIfPresent")
  void testComputeIfPresent() {
    final Map<String, Integer> map = map(pair("0", 0));

    assertEquals(1, computeIfPresent(new HashMap<>(map), "0", (k, v) -> v + 1));
    assertNull(computeIfPresent(new HashMap<>(map), "1", (k, v) -> 2));

    final HashMap<String, Integer> copy = new HashMap<>(map);

    assertNull(computeIfPresent(copy, "0", (k, v) -> null));
    assertNull(copy.get("0"));
  }

  @Test
  @DisplayName("concat")
  void testConcat() {
    assertEquals(list(0, 1, 2, 3), concat(list(0, 1), list(2, 3)));
    assertEquals(list(0, 1), concat(list(0, 1), list()));
  }

  @Test
  @DisplayName("difference")
  void testDifference() {
    assertEquals(set(0), difference(set(0, 1), set(1)));
    assertEquals(set(), difference(set(0, 1), set(0, 1)));
    assertEquals(set(0, 1), difference(set(0, 1), set()));
    assertEquals(set(0), difference(set(0, 1), set(1, 2)));
  }

  @Test
  @DisplayName("expand")
  void testExpand() {
    assertEquals(
        map(pair("a", map(pair("b", 0), pair("c", 1))), pair("d", map(pair("e", 2)))),
        expand(map(pair("a.b", 0), pair("a.c", 1), pair("d.e", 2)), "."));
  }

  @Test
  @DisplayName("flatten")
  void testFlatten() {
    assertEquals(
        map(pair("a.b", 0), pair("a.c", 1), pair("d.e", 2)),
        flatten(
            map(pair("a", map(pair("b", 0), pair("c", 1))), pair("d", map(pair("e", 2)))), "."));
  }

  @Test
  @DisplayName("intersection")
  void testIntersection() {
    assertEquals(set(1), intersection(set(0, 1), set(1, 2)));
    assertEquals(set(), intersection(set(0, 1), set(2, 3)));
    assertEquals(set(0, 1), intersection(set(0, 1), set(0, 1)));
  }

  @Test
  @DisplayName("merge")
  void testMerge() {
    assertEquals(
        map(pair("a", 0), pair("b", 1), pair("c", 2)),
        merge(map(pair("a", 0), pair("b", 0)), map(pair("b", 1), pair("c", 2))));
  }

  @Test
  @DisplayName("multiply")
  void testMultiply() {
    assertEquals(
        set(pair(0, 2), pair(0, 3), pair(1, 2), pair(1, 3)), multiply(set(0, 1), set(2, 3)));
  }

  @Test
  @DisplayName("put")
  void testPut() {
    assertEquals(map(pair("a", 1)), put(map(pair("a", 0)), "a", 1));
    assertEquals(map(pair("a", 0), pair("b", 1)), put(map(pair("a", 0)), "b", 1));
  }

  @Test
  @DisplayName("remove")
  void testRemove() {
    assertEquals(map(pair("a", 0)), remove(map(pair("a", 0), pair("b", 1)), "b"));
  }

  @Test
  @DisplayName("reverse")
  void testReverse() {
    assertEquals(list(3, 2, 1), stream(reverse(list(1, 2, 3))).collect(toList()));
  }

  @Test
  @DisplayName("shiftDown")
  void testShiftDown() {
    assertEquals(list(1, 1), shiftDown(list(0, 1), 1));
    assertEquals(list(0, 1), shiftDown(list(0, 1), 0));
    assertEquals(list(1, 1), shiftDown(list(0, 1), 10));
    assertEquals(list(1, 2), shiftDown(list(0, 1), 1, 2));
  }

  @Test
  @DisplayName("shiftUp")
  void testShiftUp() {
    assertEquals(list(0, 0), shiftUp(list(0, 1), 1));
    assertEquals(list(0, 1), shiftUp(list(0, 1), 0));
    assertEquals(list(0, 0), shiftUp(list(0, 1), 10));
    assertEquals(list(2, 0), shiftUp(list(0, 1), 1, 2));
  }

  @Test
  @DisplayName("union")
  void testUnion() {
    assertEquals(set(0, 1), union(set(0), set(1)));
    assertEquals(set(0), union(set(0), set(0)));
    assertEquals(set(0), union(set(0), set()));
  }
}
