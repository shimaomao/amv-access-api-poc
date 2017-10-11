package org.amv.access.util;

import com.google.common.base.Charsets;

import java.util.Base64;
import java.util.Optional;

public final class MoreBase64 {
    private MoreBase64() {
        throw new UnsupportedOperationException();
    }

    public static String toBase64OrThrow(String str) {
        return MoreBase64.toBase64(str)
                .orElseThrow(() -> new IllegalStateException("Error while encoding to base64"));
    }

    public static String fromBase64OrThrow(String str) {
        return MoreBase64.fromBase64(str)
                .orElseThrow(() -> new IllegalStateException("Error while decoding to base64"));
    }

    public static Optional<String> toBase64(String str) {
        return Optional.ofNullable(str)
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getEncoder().encodeToString(s));
    }

    public static Optional<String> fromBase64(String str) {
        return Optional.ofNullable(str)
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getDecoder().decode(s))
                .map(String::new);
    }

}
