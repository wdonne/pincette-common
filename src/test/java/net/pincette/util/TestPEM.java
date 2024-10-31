package net.pincette.util;

import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.PEM.sslContext;
import static net.pincette.util.Util.tryToDoRethrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestPEM {
  private static String readResource(final String name) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(() -> copy(TestPEM.class.getResourceAsStream(name), out));

    return out.toString();
  }

  @Test
  @DisplayName("PEM")
  void testPEM() {
    assertNotNull(sslContext(readResource("/expired_key.pem"), readResource("/expired_cert.pem")));
  }
}
