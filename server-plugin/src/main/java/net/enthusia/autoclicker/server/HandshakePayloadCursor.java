package net.enthusia.autoclicker.server;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

final class HandshakePayloadCursor {
    private final byte[] data;
    private int index;

    HandshakePayloadCursor(byte[] data) {
        this.data = data;
    }

    int readUnsignedByte() {
        if (index >= data.length) {
            throw new IllegalArgumentException("Unexpected end of handshake payload");
        }
        return data[index++] & 0xFF;
    }

    String readUtf(int maximumCharacters) {
        int length = readVarInt();
        int maximumBytes = maximumCharacters * 4;
        if (length < 0 || length > maximumBytes || index + length > data.length) {
            throw new IllegalArgumentException("Invalid handshake string length");
        }
        String value = decodeUtf(length);
        index += length;
        if (value.isBlank() || value.length() > maximumCharacters) {
            throw new IllegalArgumentException("Invalid handshake field");
        }
        return value;
    }

    boolean hasRemaining() {
        return index < data.length;
    }

    private String decodeUtf(int length) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data, index, length))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("Invalid UTF-8 handshake field", exception);
        }
    }

    private int readVarInt() {
        int value = 0;
        int position = 0;
        while (position < 32) {
            int current = readUnsignedByte();
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0) {
                return value;
            }
            position += 7;
        }
        throw new IllegalArgumentException("VarInt is too large");
    }
}
