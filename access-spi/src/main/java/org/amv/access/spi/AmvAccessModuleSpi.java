package org.amv.access.spi;

import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.core.SignedAccessCertificate;
import reactor.core.publisher.Mono;

public interface AmvAccessModuleSpi {

    Mono<Boolean> isValidNonceAuth(NonceAuthentication nonceAuthentication, String publicKeyBase64);

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

    Mono<SignedAccessCertificate> signAccessCertificate(SignCertificateRequest signCertificateRequest);

    // TODO: maybe remove
    Mono<String> generateSignature(String messageBase64, String privateKeyBase64);

    // TODO: maybe remove
    Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64);
}
