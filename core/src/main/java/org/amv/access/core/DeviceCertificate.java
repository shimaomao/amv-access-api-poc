package org.amv.access.core;

import com.google.common.base.Charsets;

import java.util.Base64;
import java.util.Optional;

public interface DeviceCertificate {
    Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    String getCertificate();

    String getSignedCertificate();

    default String getCertificateBase64() {
        return Optional.ofNullable(getCertificate())
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getEncoder().encodeToString(s))
                .orElse(null);
    }

    default String getSignedCertificateBase64() {
        return Optional.ofNullable(getSignedCertificate())
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getEncoder().encodeToString(s))
                .orElse(null);
    }
}
