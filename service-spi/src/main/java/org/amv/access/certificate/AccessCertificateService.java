package org.amv.access.certificate;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.auth.DeviceNonceAuthentication;
import org.amv.access.auth.IssuerNonceAuthentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface AccessCertificateService {
    Flux<SignedAccessCertificateResource> getAccessCertificates(DeviceNonceAuthentication auth);

    Mono<AccessCertificateResource> createAccessCertificate(IssuerNonceAuthentication auth, CreateAccessCertificateContext request);

    Mono<Boolean> addAccessCertificateSignatures(IssuerNonceAuthentication auth, AddAccessCertificateSignaturesContext request);

    Mono<Boolean> revokeAccessCertificate(IssuerNonceAuthentication nonceAuthentication, RevokeAccessCertificateContext request);

    Mono<SignedAccessCertificateResource> signAccessCertificate(AccessCertificateResource accessCertificateResource, String privateKeyBase64);

    Mono<String> createSignature(String messageBase64, String privateKeyBase64);

    Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64);

    @Value
    @Builder(builderClassName = "Builder")
    class RevokeAccessCertificateContext {
        @NonNull
        private UUID accessCertificateId;
    }

    @Value
    @Builder(builderClassName = "Builder")
    class AddAccessCertificateSignaturesContext {
        @NonNull
        private UUID accessCertificateId;
        @NonNull
        private String vehicleAccessCertificateSignatureBase64;
        @NonNull
        private String deviceAccessCertificateSignatureBase64;
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
