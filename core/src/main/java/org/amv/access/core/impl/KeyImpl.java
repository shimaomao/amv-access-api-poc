package org.amv.access.core.impl;

import org.amv.access.core.Key;
import org.amv.access.util.MoreBase64;
import org.amv.access.util.MoreHex;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class KeyImpl implements Key {
    public static KeyImpl fromBase64(String keyBase64) {
        requireNonNull(keyBase64);
        return fromHex(MoreBase64.decodeBase64AsHex(keyBase64));
    }

    public static KeyImpl fromHex(String keyHex) {
        requireNonNull(keyHex);
        checkArgument(MoreHex.isHex(keyHex));

        return new KeyImpl(keyHex, MoreBase64.encodeHexAsBase64(keyHex));
    }

    private final String hex;
    private final String base64;

    private KeyImpl(String hex, String base64) {
        this.hex = requireNonNull(hex);
        this.base64 = requireNonNull(base64);
    }

    @Override
    public String toHex() {
        return hex;
    }

    @Override
    public String toBase64() {
        return base64;
    }
}
