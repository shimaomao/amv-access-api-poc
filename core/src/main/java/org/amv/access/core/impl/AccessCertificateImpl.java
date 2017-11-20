package org.amv.access.core.impl;

import lombok.*;
import org.amv.access.core.*;

import java.time.LocalDateTime;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class AccessCertificateImpl implements AccessCertificate {
    @NonNull
    private String uuid;
    @NonNull
    private Issuer issuer;
    @NonNull
    private Application application;
    @NonNull
    private Device device;
    @NonNull
    private Vehicle vehicle;

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
