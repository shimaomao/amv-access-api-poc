package org.amv.access.core.impl;

import lombok.*;
import org.amv.access.core.SignedAccessCertificate;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class SignedAccessCertificateImpl implements SignedAccessCertificate {

    @NonNull
    private String deviceAccessCertificateBase64;

    @NonNull
    private String deviceAccessCertificateSignatureBase64;

    // base64(vehicleCert + sign(vehicleCert, issuerPrivateKey))
    @NonNull
    private String signedVehicleAccessCertificateBase64;

    @NonNull
    private String vehicleAccessCertificateBase64;

    @NonNull
    private String vehicleAccessCertificateSignatureBase64;

    // base64(deviceCert + sign(deviceCert, issuerPrivateKey))
    @NonNull
    private String signedDeviceAccessCertificateBase64;

}
