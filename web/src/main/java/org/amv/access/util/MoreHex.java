package org.amv.access.util;

import com.google.common.base.CharMatcher;

public final class MoreHex {
    private static final CharMatcher HEX_MATCHER = CharMatcher.JAVA_DIGIT.or(CharMatcher.anyOf("abcdef"))
            .precomputed();

    private MoreHex() {
        throw new UnsupportedOperationException();
    }

    public static boolean isHex(String str) {
        return HEX_MATCHER.matchesAllOf(str);
    }

}
