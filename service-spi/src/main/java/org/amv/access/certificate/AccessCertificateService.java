package org.amv.access.certificate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.AccessCertificate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AccessCertificateService {
    Flux<AccessCertificate> getAccessCertificates(NonceAuthentication auth, GetAccessCertificateContext request);

    Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateContext request);

    Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication, RevokeAccessCertificateContext request);

    @Value
    @Builder(builderClassName = "Builder")
    class GetAccessCertificateContext {
        @NonNull
        private String deviceSerialNumber;
    }

    @Value
    @Builder(builderClassName = "Builder")
    class RevokeAccessCertificateContext {
        @NonNull
        private String deviceSerialNumber;
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
        private LocalDateTime validityStart;

        @NonNull
        private LocalDateTime validityEnd;
    }

}
