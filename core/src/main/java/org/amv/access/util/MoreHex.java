package org.amv.access.util;

import com.google.common.base.CharMatcher;

public final class MoreHex {
    private static final CharMatcher HEX_MATCHER = CharMatcher.anyOf("0123456789")
            .or(CharMatcher.anyOf("abcdef"))
            .or(CharMatcher.anyOf("ABCDEF"))
            .precomputed();

    private MoreHex() {
        throw new UnsupportedOperationException();
    }

    public static boolean isHex(String str) {
        return str != null && HEX_MATCHER.matchesAllOf(str);
    }

}
