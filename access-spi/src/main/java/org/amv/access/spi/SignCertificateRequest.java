package org.amv.access.spi;

import org.amv.access.core.AccessCertificate;
import org.amv.access.core.Key;

import java.util.Optional;

public interface SignCertificateRequest {
    AccessCertificate getAccessCertificate();

    Key getPrivateKey();

    /**
     * If a public key is given, the consumer
     * can optionally verify the signature.
     *
     * @return an optional public key
     */
    default Optional<Key> getPublicKey() {
        return Optional.empty();
    }
}
