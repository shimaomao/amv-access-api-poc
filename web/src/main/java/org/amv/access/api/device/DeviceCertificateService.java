package org.amv.access.api.device;

import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.model.DeviceCertificateEntity;
import reactor.core.publisher.Mono;

public interface DeviceCertificateService {
    Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest request);

    //Mono<Void> revokeDeviceCertificate(RevokeDeviceCertificateRequest request);
}
