package org.amv.access.core.impl;

import lombok.*;
import org.amv.access.core.AccessCertificate;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class AccessCertificateImpl implements AccessCertificate {
    @NonNull
    private String vehicleAccessCertificateBase64;

    @NonNull
    private String deviceAccessCertificateBase64;
}
