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

    @NonNull
    private LocalDateTime validFrom;

    @NonNull
    private LocalDateTime validUntil;

    // TODO: this value is just for human inspection - not really needed.
    @NonNull
    private String vehicleAccessCertificateBase64;
    // TODO: this value is just for human inspection - not really needed.
    @NonNull
    private String vehicleAccessCertificateSignatureBase64;

    /**
     * base64(vehicleCert + sign(vehicleCert, issuerPrivateKey))
     */
    @NonNull
    private String fullVehicleAccessCertificateBase64;

    // TODO: this value is just for human inspection - not really needed.
    @NonNull
    private String deviceAccessCertificateBase64;
    // TODO: this value is just for human inspection - not really needed.
    @NonNull
    private String deviceAccessCertificateSignatureBase64;

    /**
     * base64(deviceCert + sign(deviceCert, issuerPrivateKey))
     */
    @NonNull
    private String fullDeviceAccessCertificateBase64;
}
