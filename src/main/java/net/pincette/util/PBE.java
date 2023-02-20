package net.pincette.util;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static net.pincette.util.Random.randomBytes;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;

import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for password based encryption.
 *
 * @author Werner Donné
 * @since 2.3
 */
public class PBE {
  private static final AlgorithmParameterSpec PARAMETER_SPEC = parameterSpec();

  private PBE() {}

  private static byte[] crypt(final byte[] bytes, final SecretKey key, final int mode) {
    return tryToGetRethrow(() -> Cipher.getInstance("AES/GCM/NoPadding"))
        .map(
            cipher -> {
              tryToDoRethrow(() -> cipher.init(mode, key, PARAMETER_SPEC));

              return cipher;
            })
        .flatMap(cipher -> tryToGetRethrow(() -> cipher.doFinal(bytes)))
        .orElse(null);
  }

  public static byte[] decrypt(final byte[] bytes, final SecretKey key) {
    return crypt(bytes, key, DECRYPT_MODE);
  }

  public static byte[] encrypt(final byte[] bytes, final SecretKey key) {
    return crypt(bytes, key, ENCRYPT_MODE);
  }

  /**
   * Generates a key with a random salt.
   *
   * @param password the given password.
   * @return The generated key.
   */
  public static SecretKey key(final char[] password) {
    return key(password, null);
  }

  /**
   * Generates a key.
   *
   * @param password the given password.
   * @param salt bring your own salt. It can be <code>null</code>, in which case a salt is
   *     generated.
   * @return The generated key.
   */
  public static SecretKey key(final char[] password, final byte[] salt) {
    return tryToGetRethrow(() -> SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512"))
        .flatMap(
            factory ->
                tryToGetRethrow(
                    () ->
                        factory.generateSecret(
                            new PBEKeySpec(
                                password, salt != null ? salt : randomBytes(64), 65536, 256))))
        .map(SecretKey::getEncoded)
        .map(encoded -> new SecretKeySpec(encoded, "AES"))
        .orElse(null);
  }

  private static AlgorithmParameterSpec parameterSpec() {
    return new GCMParameterSpec(16 * 8, randomBytes(32));
  }
}
