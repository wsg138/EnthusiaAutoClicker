package net.enthusia.autoclicker.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class HandshakePayloadCursorTest {
    @Test
    void readsUnsignedBytesAcrossSignedByteBoundary() {
        HandshakePayloadCursor cursor = new HandshakePayloadCursor(new byte[] {0, 127, (byte) 128, (byte) 255});

        assertEquals(0, cursor.readUnsignedByte());
        assertEquals(127, cursor.readUnsignedByte());
        assertEquals(128, cursor.readUnsignedByte());
        assertEquals(255, cursor.readUnsignedByte());
        assertFalse(cursor.hasRemaining());
    }

    @Test
    void readUnsignedByteFailsClosedAtEndOfPayload() {
        HandshakePayloadCursor cursor = new HandshakePayloadCursor(new byte[] {1});
        cursor.readUnsignedByte();

        assertThrows(IllegalArgumentException.class, cursor::readUnsignedByte);
    }

    @Test
    void readsUtfWithMultiByteVarIntLength() {
        String value = "x".repeat(130);
        HandshakePayloadCursor cursor = new HandshakePayloadCursor(utf(value));

        assertEquals(value, cursor.readUtf(130));
        assertFalse(cursor.hasRemaining());
    }

    @Test
    void rejectsBlankOversizedTruncatedAndMalformedUtfFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new HandshakePayloadCursor(utf("   ")).readUtf(10));
        assertThrows(IllegalArgumentException.class,
                () -> new HandshakePayloadCursor(utf("abcd")).readUtf(3));
        assertThrows(IllegalArgumentException.class,
                () -> new HandshakePayloadCursor(new byte[] {3, 'a'}).readUtf(10));
        assertThrows(IllegalArgumentException.class,
                () -> new HandshakePayloadCursor(new byte[] {2, (byte) 0xC3, 0x28}).readUtf(10));
    }

    @Test
    void rejectsVarIntLongerThanThirtyTwoBits() {
        byte[] malformed = {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80};
        HandshakePayloadCursor cursor = new HandshakePayloadCursor(malformed);

        assertThrows(IllegalArgumentException.class, () -> cursor.readUtf(10));
    }

    @Test
    void hasRemainingTracksConsumedContent() {
        HandshakePayloadCursor cursor = new HandshakePayloadCursor(new byte[] {1, 2});
        assertTrue(cursor.hasRemaining());
        cursor.readUnsignedByte();
        assertTrue(cursor.hasRemaining());
        cursor.readUnsignedByte();
        assertFalse(cursor.hasRemaining());
    }

    private static byte[] utf(String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int remaining = encoded.length;
        do {
            int next = remaining & 0x7F;
            remaining >>>= 7;
            if (remaining != 0) {
                next |= 0x80;
            }
            output.write(next);
        } while (remaining != 0);
        output.writeBytes(encoded);
        return output.toByteArray();
    }
}
