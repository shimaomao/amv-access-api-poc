package org.amv.access.core.impl;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Application;
import org.amv.access.core.Device;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.core.Issuer;

@Value
@Builder(builderClassName = "Builder")
public class DeviceCertificateImpl implements DeviceCertificate {
    @NonNull
    private Issuer issuer;

    @NonNull
    private Application application;

    @NonNull
    private Device device;

    @NonNull
    private String signedDeviceCertificateBase64;
}
