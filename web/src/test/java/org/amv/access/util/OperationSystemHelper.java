package org.amv.access.util;

public final class OperationSystemHelper {
    private OperationSystemHelper() {
        throw new UnsupportedOperationException();
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}