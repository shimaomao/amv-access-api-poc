package org.amv.access.spi;

import org.amv.access.core.AccessCertificate;

import java.util.Optional;

public interface SignCertificateRequest {
    AccessCertificate getAccessCertificate();

    String getPrivateKeyBase64();

    /**
     * If a public key is given, the consumer
     * can optionally verify the signature.
     *
     * @return an optional public key
     */
    default Optional<String> getPublicKeyBase64() {
        return Optional.empty();
    }
}
