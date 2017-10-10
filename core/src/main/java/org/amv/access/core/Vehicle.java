package org.amv.access.core;

import com.google.common.base.Charsets;

import java.util.Base64;
import java.util.Optional;

public interface Vehicle {
    String getSerialNumber();

    String getPublicKey();

    default String getPublicKeyBase64() {
        return Optional.ofNullable(getPublicKey())
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getEncoder().encodeToString(s))
                .orElse(null);
    }
}
