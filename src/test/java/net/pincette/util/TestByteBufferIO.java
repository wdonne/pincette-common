package net.pincette.util;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.Channels.newChannel;
import static net.pincette.io.StreamConnector.copy;
import static net.pincette.util.Util.tryToDoRethrow;
import static net.pincette.util.Util.tryToGetRethrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import net.pincette.io.ByteBufferInputStream;
import net.pincette.io.ByteBufferOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestByteBufferIO {
  private static byte[] getArray(final List<ByteBuffer> buffers) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    tryToDoRethrow(() -> copy(new ByteBufferInputStream(buffers), out));

    return out.toByteArray();
  }

  private static ByteBuffer newBuffer(final List<ByteBuffer> buffers) {
    final ByteBuffer buffer = allocate(0x1000);

    buffers.add(buffer);

    return buffer;
  }

  private static List<ByteBuffer> read(final InputStream in) {
    final ReadableByteChannel channel = newChannel(in);
    final List<ByteBuffer> result = new ArrayList<>();

    while (tryToGetRethrow(() -> channel.read(newBuffer(result))).orElse(-1) != -1)
      ;

    return result.stream().map(b -> b.position(0)).toList();
  }

  @Test
  @DisplayName("ByteBuffer IO")
  void test() {
    final List<ByteBuffer> in = read(TestByteBufferIO.class.getResourceAsStream("/file.pdf"));
    final List<ByteBuffer> out = new ArrayList<>();

    tryToDoRethrow(
        () ->
            copy(new ByteBufferInputStream(in), new ByteBufferOutputStream(() -> newBuffer(out))));

    assertArrayEquals(getArray(in), getArray(out));
  }
}
