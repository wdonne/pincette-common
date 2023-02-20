package net.pincette.util;

import static java.security.SecureRandom.getSeed;
import static net.pincette.util.Util.tryToGetRethrow;

import java.security.SecureRandom;

/**
 * Utility for generating random data.
 *
 * @author Werner Donné
 * @since 2.3
 */
public class Random {
  private static final SecureRandom SECURE_RANDOM =
      tryToGetRethrow(SecureRandom::getInstanceStrong).map(Random::seed).orElse(null);

  private Random() {}

  public static byte[] randomBytes(final int size) {
    final byte[] b = new byte[size];

    SECURE_RANDOM.nextBytes(b);

    return b;
  }

  public static char[] randomChars(final int size) {
    final byte[] bytes = randomBytes(size * 2);
    final char[] chars = new char[size];

    for (int i = 0; i < chars.length; ++i) {
      chars[i] = (char) ((bytes[i * 2] & 0xff) + (bytes[i * 2 + 1] << 8));
    }
    return chars;
  }

  private static SecureRandom seed(final SecureRandom random) {
    random.setSeed(getSeed(64));

    return random;
  }
}
