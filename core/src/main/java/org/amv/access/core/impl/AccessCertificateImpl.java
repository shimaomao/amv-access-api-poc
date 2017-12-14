package org.amv.access.core.impl;

import lombok.*;
import org.amv.access.core.*;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class AccessCertificateImpl implements AccessCertificate {
    @NonNull
    private String uuid;
    @NonNull
    private String name;
    /*@NonNull
    private Issuer issuer;
    @NonNull
    private Application application;
    @NonNull
    private Device device;
    @NonNull
    private Vehicle vehicle;*/

    @NonNull
    private String vehicleAccessCertificateBase64;

    @NonNull
    private String deviceAccessCertificateBase64;
}
