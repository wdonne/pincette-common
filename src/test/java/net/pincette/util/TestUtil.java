package net.pincette.util;

import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.regex.Pattern.compile;
import static net.pincette.util.Collections.list;
import static net.pincette.util.ShadowString.shadow;
import static net.pincette.util.Util.tryToGetForever;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.pincette.util.Util.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestUtil {
  private static void allPaths(
      final String path, final String delimiter, final List<String> expected) {
    assertEquals(expected, Util.allPaths(path, delimiter).toList());
  }

  @Test
  @DisplayName("allPaths")
  void allPaths() {
    allPaths("/a/b/c", "/", list("/a/b/c", "/a/b", "/a", "/"));
    allPaths("/a/b/c/", "/", list("/a/b/c", "/a/b", "/a", "/"));
    allPaths("a/b/c", "/", list("a/b/c", "a/b", "a"));
    allPaths("a/b/c/", "/", list("a/b/c", "a/b", "a"));
    allPaths("a.b.c.", ".", list("a.b.c", "a.b", "a"));
    allPaths("a", ".", list("a"));
    allPaths("/", "/", list("/"));
    allPaths("", "/", list());
  }

  @Test
  @DisplayName("canonicalPath")
  void canonicalPath() {
    assertEquals("/a/b/c", Util.canonicalPath("/a/b/c", "/"));
    assertEquals("/a/c", Util.canonicalPath("/a/b/../c", "/"));
    assertEquals("/c", Util.canonicalPath("/a/b/../../c", "/"));
    assertEquals("/", Util.canonicalPath("/a/b/../../", "/"));
    assertEquals("/", Util.canonicalPath("/a/b/../..", "/"));
    assertEquals("/c", Util.canonicalPath("/a/../b/../c", "/"));
  }

  @Test
  @DisplayName("getParent")
  void getParent() {
    assertEquals("/a/b", Util.getParent("/a/b/c", "/"));
    assertEquals("/a/b", Util.getParent("/a/b/c/", "/"));
    assertEquals("/", Util.getParent("/a", "/"));
    assertEquals("/", Util.getParent("/a/", "/"));
    assertEquals("/", Util.getParent("/", "/"));
    assertEquals("", Util.getParent("", "/"));
    assertEquals("a/b", Util.getParent("a/b/c", "/"));
    assertEquals("", Util.getParent("a", "/"));
  }

  @Test
  @DisplayName("isEmail")
  void isEmail() {
    assertTrue(Util.isEmail("a@re.be"));
    assertTrue(Util.isEmail("A@re.be"));
    assertTrue(Util.isEmail("A.b@re.be"));
    assertTrue(Util.isEmail("aAb%sdfsY@a.re.be"));
    assertTrue(Util.isEmail("0-_aAbsdfsY@a22.r-e.be"));
    assertTrue(Util.isEmail("0-_aAbsdfsY@a22.R-e.be"));
    assertFalse(Util.isEmail("&@re.be"));
    assertFalse(Util.isEmail("a@r_e.be"));
  }

  @Test
  @DisplayName("isInstant")
  void isInstant() {
    assertTrue(Util.isInstant("2021-01-24T12:11:42.090045Z"));
    assertTrue(Util.isInstant("2021-01-24T12:11:42.090Z"));
    assertTrue(Util.isInstant("2021-01-24T12:11:42Z"));
    assertTrue(Util.isInstant("2021-01-24T12:11:42"));
    assertTrue(Util.isInstant("2021-01-24T12:11:42+00:00"));
    assertFalse(Util.isInstant("2021-01-24T12:11"));
    assertFalse(Util.isInstant("2021-01-24"));
  }

  @Test
  @DisplayName("readLineConfig")
  void readLineConfig() {
    assertEquals(
        list("line1", "line2 continue", "line3 continue", "line4"),
        Util.readLineConfig(
                list(
                    "#comment",
                    "line1",
                    "line2 \\\n",
                    "continue",
                    "line3 \\\r\n",
                    "continue",
                    "line4#comment")
                    .stream())
            .toList());
  }

  @Test
  @DisplayName("segments1")
  void segments1() {
    assertEquals(list(shadow("a"), shadow("b"), shadow("c")), Util.segments("a#b#c", "#").toList());
    assertEquals(list(shadow("a"), shadow("b")), Util.segments("a#b#", "#").toList());
    assertEquals(list(shadow("a")), Util.segments("a", "#").toList());
    assertEquals(list(shadow(""), shadow("a")), Util.segments("#a", "#").toList());
    assertEquals(list(shadow("")), Util.segments("", "#").toList());
    assertEquals(list(shadow("")), Util.segments("#", "#").toList());
  }

  @Test
  @DisplayName("segments2")
  void segments2() {
    assertEquals(
        list(shadow("a"), shadow("b"), shadow("c")), Util.segments("a#b#c", compile("#")).toList());
    assertEquals(
        list(shadow("a"), shadow("b"), shadow("c")),
        Util.segments("a# b # c", compile(" *# *")).toList());
    assertEquals(list(shadow("a"), shadow("b")), Util.segments("a#b#", compile("#")).toList());
    assertEquals(list(shadow("a")), Util.segments("a", compile("#")).toList());
    assertEquals(list(shadow(""), shadow("a")), Util.segments("#a", compile("#")).toList());
    assertEquals(list(shadow("")), Util.segments("", compile("#")).toList());
    assertEquals(list(shadow("")), Util.segments("#", compile("#")).toList());
  }

  @Test
  @DisplayName("tryToGetForever1")
  void tryToGetForever1() {
    final State<Integer> called = new State<>(0);

    assertEquals(
        0,
        tryToGetForever(
                () -> {
                  if (called.set(called.get() + 1) < 3) {
                    throw new GeneralException("test");
                  }

                  return completedFuture(0);
                },
                ofSeconds(1))
            .toCompletableFuture()
            .join());
  }

  @Test
  @DisplayName("tryToGetForever2")
  void tryToGetForever2() {
    final State<Integer> called = new State<>(0);

    assertEquals(
        0,
        tryToGetForever(
                () ->
                    supplyAsync(() -> 0)
                        .thenApply(
                            v -> {
                              if (called.set(called.get() + 1) < 3) {
                                throw new GeneralException("test");
                              }

                              return v;
                            }),
                ofSeconds(1))
            .toCompletableFuture()
            .join());
  }
}
