package net.enthusia.autoclicker.server.api;

/** Describes how the server classified the most recent client handshake observation. */
public enum ClientEvidenceValidation {
    NOT_OBSERVED,
    VALID,
    UNSUPPORTED_PROTOCOL,
    MALFORMED
}
