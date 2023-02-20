package net.pincette.util;

import static net.pincette.util.PBE.decrypt;
import static net.pincette.util.PBE.encrypt;
import static net.pincette.util.PBE.key;
import static net.pincette.util.Random.randomBytes;
import static net.pincette.util.Random.randomChars;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestPBE {
  @Test
  @DisplayName("pbe")
  void pbe() {
    final SecretKey key = key(randomChars(128));
    final byte[] value = randomBytes(1024);

    assertArrayEquals(value, decrypt(encrypt(value, key), key));
  }
}
