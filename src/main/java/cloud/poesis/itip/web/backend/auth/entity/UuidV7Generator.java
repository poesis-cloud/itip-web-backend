package cloud.poesis.itip.web.backend.auth.entity;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class UuidV7Generator {

  private UuidV7Generator() {}

  static UUID generate() {
    long timestampMillis = Instant.now().toEpochMilli();
    byte[] bytes = new byte[16];
    ThreadLocalRandom.current().nextBytes(bytes);

    bytes[0] = (byte) (timestampMillis >>> 40);
    bytes[1] = (byte) (timestampMillis >>> 32);
    bytes[2] = (byte) (timestampMillis >>> 24);
    bytes[3] = (byte) (timestampMillis >>> 16);
    bytes[4] = (byte) (timestampMillis >>> 8);
    bytes[5] = (byte) timestampMillis;

    bytes[6] = (byte) ((bytes[6] & 0x0F) | 0x70);
    bytes[8] = (byte) ((bytes[8] & 0x3F) | 0x80);

    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    long mostSignificantBits = byteBuffer.getLong();
    long leastSignificantBits = byteBuffer.getLong();
    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
