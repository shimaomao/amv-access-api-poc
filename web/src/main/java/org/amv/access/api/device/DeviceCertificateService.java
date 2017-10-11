package org.amv.access.api.device;

import lombok.Builder;
import lombok.Value;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.core.DeviceCertificate;
import reactor.core.publisher.Mono;

public interface DeviceCertificateService {

    @Value
    @Builder(builderClassName = "Builder")
    class CreateDeviceCertificateRequest {

        private String appId;

        private String devicePublicKeyBase64;

        private String deviceName;
    }

    Mono<DeviceCertificate> createDeviceCertificate(ApplicationAuthentication auth, CreateDeviceCertificateRequest request);

    //Mono<Void> revokeDeviceCertificate(RevokeDeviceCertificateRequest request);
}
