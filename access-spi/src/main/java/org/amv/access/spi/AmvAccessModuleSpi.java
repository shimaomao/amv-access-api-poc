package org.amv.access.spi;

import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.Device;
import org.amv.access.core.DeviceCertificate;
import reactor.core.publisher.Mono;

public interface AmvAccessModuleSpi {

    Mono<Boolean> isValidNonceAuth(NonceAuthentication nonceAuthentication, Device device);

    /**
     * Create a device certificate to be stored on the users device.
     *
     * @param deviceCertificateRequest the incoming request entity
     * @return a device certificate for the device
     */
    Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest deviceCertificateRequest);

    /**
     * Create an access certificate consisting of a vehicle and a device access certificate.
     * This certificates will enable users to open vehicles with their devices.
     *
     * @param accessCertificateRequest the incoming request entity
     * @return an access certificate for the device
     */
    Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest accessCertificateRequest);

}
