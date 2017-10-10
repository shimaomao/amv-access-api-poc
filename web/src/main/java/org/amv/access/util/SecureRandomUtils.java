package org.amv.access.util;

import org.apache.commons.codec.binary.Hex;

import java.security.SecureRandom;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

public final class SecureRandomUtils {
    private static final Random RANDOM = new SecureRandom();

    private SecureRandomUtils() {
        throw new UnsupportedOperationException();
    }


    public static byte[] nextBytes(final int count) {
        checkArgument(count >= 0, "Count cannot be negative.");

        final byte[] result = new byte[count];
        RANDOM.nextBytes(result);
        return result;
    }

    public static String generateRandomHexString(int count) {
        checkArgument(count >= 0, "Count cannot be negative.");
        return Hex.encodeHexString(nextBytes(count));
    }

    public static String generateRandomSerial() {
        return generateRandomHexString(9);
    }

    public static String generateRandomAppId() {
        return generateRandomHexString(12);
    }
}