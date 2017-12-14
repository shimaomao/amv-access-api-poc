package org.amv.access.util;

import com.google.common.base.Charsets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.Base64;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Base64.isBase64;

public final class MoreBase64 {
    private MoreBase64() {
        throw new UnsupportedOperationException();
    }

    public static String toBase64OrThrow(String str) {
        return MoreBase64.toBase64(str)
                .orElseThrow(() -> new IllegalStateException("Error while encoding to base64: " + str));
    }

    public static String fromBase64OrThrow(String str) {
        return MoreBase64.fromBase64(str)
                .orElseThrow(() -> new IllegalStateException("Error while decoding to base64: " + str));
    }

    public static Optional<String> toBase64(String str) {
        return Optional.ofNullable(str)
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getEncoder().encode(s))
                .map(s -> new String(s, Charsets.UTF_8));
    }

    public static Optional<String> fromBase64(String str) {
        return Optional.ofNullable(str)
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getDecoder().decode(s))
                .map(s -> new String(s, Charsets.UTF_8));
    }

    public static String decodeBase64AsHex(String key) {
        requireNonNull(key, "`key` must not be null");
        checkArgument(isBase64(key), "`key` must be base64");

        byte[] keyInBytes = fromBase64(key)
                .map(s -> s.getBytes(Charsets.UTF_8))
                .orElseThrow(IllegalStateException::new);
        return Hex.encodeHexString(keyInBytes);
    }

    public static String encodeHexAsBase64(String key) {
        requireNonNull(key, "`key` must not be null");

        try {
            byte[] keyBase16 = Hex.decodeHex(key.toCharArray());
            return toBase64(new String(keyBase16, Charsets.UTF_8))
                    .orElseThrow(IllegalStateException::new);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }
}
