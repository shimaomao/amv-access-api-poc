package org.amv.access.spi;

import org.amv.access.auth.NonceAuthentication;
import org.amv.access.model.*;
import reactor.core.publisher.Mono;

public interface AmvAccessModuleSpi {

    Mono<Boolean> isValidNonceAuth(NonceAuthentication nonceAuthentication, Device device);

    /**
     * Create a device certificate to be stored on the users device.
     *
     * @param application the application requesting a certificate
     * @param device      the device the created certificate is for
     * @return a device certificate for the device
     */
    Mono<DeviceCertificate> createDeviceCertificate(
            Application application,
            Device device
    );

    /**
     * Create an access certificate consisting of a vehicle and a device access certificate.
     * This certificates will enable users to open vehicles with their devices.
     *
     * @param accessCertificateRequest the incoming request entity
     * @param device                   the device one of the created certificates is for
     * @param vehicle                  the vehicle one of the created certificates is for
     * @return an access certificate for the device
     */
    Mono<AccessCertificate> createAccessCertificate(
            AccessCertificateRequest accessCertificateRequest,
            Device device,
            Vehicle vehicle
    );

}
