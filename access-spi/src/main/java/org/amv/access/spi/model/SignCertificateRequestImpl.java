package org.amv.access.spi.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.AccessCertificate;
import org.amv.access.spi.SignCertificateRequest;

import java.util.Optional;

@Value
@Builder(builderClassName = "Builder")
public class SignCertificateRequestImpl implements SignCertificateRequest {

    @NonNull
    private String privateKeyBase64;

    @NonNull
    private AccessCertificate accessCertificate;

    private String publicKeyBase64;

    @Override
    public Optional<String> getPublicKeyBase64() {
        return Optional.ofNullable(publicKeyBase64);
    }
}
