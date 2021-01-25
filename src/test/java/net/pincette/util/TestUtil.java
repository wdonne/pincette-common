package net.pincette.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestUtil {
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
}
