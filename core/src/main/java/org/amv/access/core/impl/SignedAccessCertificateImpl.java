package org.amv.access.core.impl;

import lombok.*;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.SignedAccessCertificate;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class SignedAccessCertificateImpl implements SignedAccessCertificate {
    @NonNull
    private AccessCertificate accessCertificate;

    /**
     * base64(vehicleCert + sign(vehicleCert, issuerPrivateKey))
     */
    @NonNull
    private String signedVehicleAccessCertificateBase64;

    /**
     * base64(deviceCert + sign(deviceCert, issuerPrivateKey))
     */
    @NonNull
    private String signedDeviceAccessCertificateBase64;

}
