package net.enthusia.autoclicker.server;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

final class ClientHandshakeTestPayload {
    private ClientHandshakeTestPayload() {
    }

    static byte[] create(
            int protocol,
            String modVersion,
            String loader,
            String minecraftVersion
    ) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(protocol);
        writeUtf(output, modVersion);
        writeUtf(output, loader);
        writeUtf(output, minecraftVersion);
        return output.toByteArray();
    }

    private static void writeUtf(ByteArrayOutputStream output, String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(output, encoded.length);
        output.writeBytes(encoded);
    }

    private static void writeVarInt(ByteArrayOutputStream output, int value) {
        int remaining = value;
        do {
            int next = remaining & 0x7F;
            remaining >>>= 7;
            if (remaining != 0) {
                next |= 0x80;
            }
            output.write(next);
        } while (remaining != 0);
    }
}
