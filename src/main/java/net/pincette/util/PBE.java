package net.pincette.util;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static net.pincette.util.Util.tryToGetRethrow;

import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The class contains password-based encryption functions.
 *
 * @author Werner Donn\u00e9
 * @deprecated Not secure.
 */
@Deprecated
public class PBE {
  private static final byte[] SALT =
      new byte[] {
        (byte) 0x34,
        (byte) 0xfe,
        (byte) 0x9a,
        (byte) 0x01,
        (byte) 0x33,
        (byte) 0x12,
        (byte) 0x98,
        (byte) 0x55
      };

  private PBE() {}

  public static byte[] crypt(final byte[] data, final Cipher cipher) {
    return tryToGetRethrow(() -> cipher.doFinal(data)).orElse(null);
  }

  public static byte[] decrypt(final byte[] data, final char[] password) {
    return crypt(data, getCipher(password, false));
  }

  public static byte[] encrypt(final byte[] data, final char[] password) {
    return crypt(data, getCipher(password, true));
  }

  public static Cipher getCipher(final char[] password, final boolean encrypt) {
    return tryToGetRethrow(
            () -> {
              final Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

              cipher.init(
                  encrypt ? ENCRYPT_MODE : DECRYPT_MODE,
                  new SecretKeySpec(
                      SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                          .generateSecret(new PBEKeySpec(password, SALT, 100, 128))
                          .getEncoded(),
                      "AES"));

              return cipher;
            })
        .orElse(null);
  }

  public static InputStream getInputStream(final InputStream in, final char[] password) {
    return new CipherInputStream(in, getCipher(password, false));
  }

  public static OutputStream getOutputStream(final OutputStream out, final char[] password) {
    return new CipherOutputStream(out, getCipher(password, true));
  }
}
