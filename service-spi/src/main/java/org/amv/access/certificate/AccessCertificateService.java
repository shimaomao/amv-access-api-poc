package org.amv.access.certificate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.auth.DeviceNonceAuthentication;
import org.amv.access.auth.IssuerNonceAuthentication;
import org.amv.access.core.AccessCertificate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface AccessCertificateService {
    Flux<AccessCertificate> getAccessCertificates(DeviceNonceAuthentication auth);

    Mono<AccessCertificate> createAccessCertificate(IssuerNonceAuthentication auth, CreateAccessCertificateContext request);

    Mono<Boolean> revokeAccessCertificate(IssuerNonceAuthentication nonceAuthentication, RevokeAccessCertificateContext request);

    @Value
    @Builder(builderClassName = "Builder")
    class RevokeAccessCertificateContext {
        @NonNull
        private UUID accessCertificateId;
    }

    @Value
    @Builder(builderClassName = "Builder")
    class CreateAccessCertificateContext {
        @NonNull
        private String appId;

        @NonNull
        private String deviceSerialNumber;

        @NonNull
        private String vehicleSerialNumber;

        @NonNull
        private Instant validityStart;

        @NonNull
        private Instant validityEnd;
    }

}
