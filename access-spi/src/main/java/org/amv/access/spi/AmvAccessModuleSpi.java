package org.amv.access.spi;

import org.amv.access.model.*;
import reactor.core.publisher.Mono;

public interface AmvAccessModuleSpi {

    /**
     * Create a device certificate to be stored on the users device.
     *
     * @param deviceCertificateRequest the incoming request entity
     * @param device the device the created certificate is for
     * @return a device certificate for the device
     */
    Mono<DeviceCertificate> createDeviceCertificate(
            DeviceCertificateRequest deviceCertificateRequest,
            Device device
    );

    /**
     * Create an access certificate consisting of a vehicle and a device access certificate.
     * This certificates will enable users to open vehicles with their devices.
     *
     * @param accessCertificateRequest  the incoming request entity
     * @param device the device one of the created certificates is for
     * @param vehicle the vehicle one of the created certificates is for
     * @return an access certificate for the device
     */
    Mono<AccessCertificate> createAccessCertificate(
            AccessCertificateRequest accessCertificateRequest,
            Device device,
            Vehicle vehicle
    );

}
