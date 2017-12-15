package org.amv.access.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.util.Base64;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.codec.binary.Base64.isBase64;

public final class MoreBase64 {
    private MoreBase64() {
        throw new UnsupportedOperationException();
    }

    public static String decodeBase64AsHex(String key) {
        requireNonNull(key, "`key` must not be null");
        checkArgument(isBase64(key), "`key` must be in base64");

        byte[] keyInBytes = Base64.getDecoder().decode(key);
        return Hex.encodeHexString(keyInBytes);
    }

    public static String encodeHexAsBase64(String key) {
        requireNonNull(key, "`key` must not be null");
        requireNonNull(MoreHex.isHex(key), "`key` must be in hex");

        try {
            byte[] keyBase16 = Hex.decodeHex(key.toCharArray());
            return Base64.getEncoder().encodeToString(keyBase16);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }
}
