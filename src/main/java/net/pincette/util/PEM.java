package net.pincette.util;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.security.KeyFactory.getInstance;
import static java.util.Arrays.stream;
import static java.util.Base64.getDecoder;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static net.pincette.util.Util.tryToGetSilent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import net.pincette.function.BiFunctionWithException;

/**
 * PEM utilities.
 *
 * @since 2.5.0
 * @author Werner Donné
 */
public class PEM {
  private static final char[] FAKE_PASSWORD = randomUUID().toString().toCharArray();

  private PEM() {}

  public static Stream<Certificate> certificateChain(final String pem) {
    final BufferedInputStream in =
        new BufferedInputStream(new ByteArrayInputStream(pem.getBytes(US_ASCII)));

    return tryToGetRethrow(() -> CertificateFactory.getInstance("X.509"))
        .map(
            factory ->
                StreamUtil.generate(
                    () ->
                        tryToGetRethrow(in::available)
                            .filter(a -> a > 0)
                            .flatMap(a -> tryToGetRethrow(() -> factory.generateCertificate(in)))))
        .orElseGet(Stream::of);
  }

  private static byte[] decode(final String key) {
    return getDecoder().decode(extractKey(key));
  }

  private static String extractKey(final String s) {
    return stream(s.split("\\n")).filter(line -> !line.startsWith("-----")).collect(joining());
  }

  private static <T> T generate(
      final EncodedKeySpec spec, final BiFunctionWithException<KeyFactory, EncodedKeySpec, T> gen) {
    return tryToGetSilent(() -> gen.apply(getInstance("RSA"), spec))
        .orElseGet(() -> tryToGetRethrow(() -> gen.apply(getInstance("EC"), spec)).orElse(null));
  }

  private static KeyManager[] keyManagers(final KeyStore keyStore) {
    return tryToGetRethrow(
            () -> {
              final KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");

              factory.init(keyStore, FAKE_PASSWORD);

              return factory.getKeyManagers();
            })
        .orElseGet(() -> new KeyManager[0]);
  }

  private static Optional<KeyStore> keyStore(final String key, final String certificateChain) {
    return tryToGetRethrow(
        () -> {
          final KeyStore store = KeyStore.getInstance("jks");

          store.load(null, FAKE_PASSWORD);
          store.setKeyEntry(
              "entry",
              privateKey(key),
              FAKE_PASSWORD,
              certificateChain(certificateChain).toArray(Certificate[]::new));

          return store;
        });
  }

  public static PrivateKey privateKey(final String key) {
    return generate(new PKCS8EncodedKeySpec(decode(key)), KeyFactory::generatePrivate);
  }

  public static PublicKey publicKey(final String key) {
    return generate(new X509EncodedKeySpec(decode(key)), KeyFactory::generatePublic);
  }

  public static SSLContext sslContext(final String privateKey, final String certificateChain) {
    return keyStore(privateKey, certificateChain)
        .flatMap(
            store ->
                tryToGetRethrow(() -> SSLContext.getInstance("TLSv1.2"))
                    .map(
                        context -> {
                          tryToDoRethrow(() -> context.init(keyManagers(store), null, null));
                          return context;
                        }))
        .orElse(null);
  }
}
