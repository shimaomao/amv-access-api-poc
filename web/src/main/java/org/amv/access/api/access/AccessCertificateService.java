package org.amv.access.api.access;

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
    Flux<AccessCertificate> getAccessCertificates(NonceAuthentication auth, GetAccessCertificateRequest request);

    Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest request);

    Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication, RevokeAccessCertificateRequest request);

    @Value
    @Builder(builderClassName = "Builder")
    class GetAccessCertificateRequest {
        @NonNull
        private String deviceSerialNumber;
    }

    @Value
    @Builder(builderClassName = "Builder")
    class RevokeAccessCertificateRequest {
        @NonNull
        private String deviceSerialNumber;
        @NonNull
        private UUID accessCertificateId;
    }

    @Value
    @Builder(builderClassName = "Builder")
    class CreateAccessCertificateRequest {
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
