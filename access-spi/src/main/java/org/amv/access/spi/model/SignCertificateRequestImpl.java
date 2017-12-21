package org.amv.access.spi.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.Key;
import org.amv.access.spi.SignCertificateRequest;

import java.util.Optional;

@Value
@Builder(builderClassName = "Builder")
public class SignCertificateRequestImpl implements SignCertificateRequest {

    @NonNull
    private Key privateKey;

    @NonNull
    private AccessCertificate accessCertificate;

    private Key publicKey;

    @Override
    public Optional<Key> getPublicKey() {
        return Optional.ofNullable(publicKey);
    }
}
