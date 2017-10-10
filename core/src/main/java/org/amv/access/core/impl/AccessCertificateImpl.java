package org.amv.access.core.impl;

import lombok.*;
import org.amv.access.core.*;

import java.time.LocalDateTime;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class AccessCertificateImpl implements AccessCertificate {
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

    @NonNull
    private String signedVehicleAccessCertificateBase64;

    @NonNull
    private String signedDeviceAccessCertificateBase64;
}
