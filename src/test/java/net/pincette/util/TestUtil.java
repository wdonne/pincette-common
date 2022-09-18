package net.pincette.util;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static net.pincette.util.Collections.list;
import static net.pincette.util.ShadowString.shadow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestUtil {
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
  @DisplayName("segments1")
  void segments1() {
    assertEquals(
        list(shadow("a"), shadow("b"), shadow("c")), Util.segments("a#b#c", "#").collect(toList()));
    assertEquals(list(shadow("a"), shadow("b")), Util.segments("a#b#", "#").collect(toList()));
    assertEquals(list(shadow("a")), Util.segments("a", "#").collect(toList()));
    assertEquals(list(shadow(""), shadow("a")), Util.segments("#a", "#").collect(toList()));
    assertEquals(list(shadow("")), Util.segments("", "#").collect(toList()));
    assertEquals(list(shadow("")), Util.segments("#", "#").collect(toList()));
  }

  @Test
  @DisplayName("segments2")
  void segments2() {
    assertEquals(
        list(shadow("a"), shadow("b"), shadow("c")),
        Util.segments("a#b#c", compile("#")).collect(toList()));
    assertEquals(
        list(shadow("a"), shadow("b"), shadow("c")),
        Util.segments("a# b # c", compile(" *# *")).collect(toList()));
    assertEquals(
        list(shadow("a"), shadow("b")), Util.segments("a#b#", compile("#")).collect(toList()));
    assertEquals(list(shadow("a")), Util.segments("a", compile("#")).collect(toList()));
    assertEquals(
        list(shadow(""), shadow("a")), Util.segments("#a", compile("#")).collect(toList()));
    assertEquals(list(shadow("")), Util.segments("", compile("#")).collect(toList()));
    assertEquals(list(shadow("")), Util.segments("#", compile("#")).collect(toList()));
  }
}
