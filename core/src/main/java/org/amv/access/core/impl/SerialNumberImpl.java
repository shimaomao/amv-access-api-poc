package org.amv.access.core.impl;

import org.amv.access.core.SerialNumber;
import org.amv.access.util.MoreHex;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SerialNumberImpl implements SerialNumber {
    public static SerialNumberImpl fromHex(String serialNumberHex) {
        requireNonNull(serialNumberHex);
        checkArgument(MoreHex.isHex(serialNumberHex));

        return new SerialNumberImpl(serialNumberHex);
    }

    private final String hex;

    private SerialNumberImpl(String hex) {
        this.hex = requireNonNull(hex);
    }

    @Override
    public String toHex() {
        return hex;
    }
}
