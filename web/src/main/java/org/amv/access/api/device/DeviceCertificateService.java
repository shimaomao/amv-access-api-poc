package org.amv.access.api.device;

import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.model.DeviceCertificate;
import reactor.core.publisher.Mono;

public interface DeviceCertificateService {
    Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest request);

    //Mono<Void> revokeDeviceCertificate(RevokeDeviceCertificateRequest request);
}
