package org.amv.access.spi.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.*;
import org.amv.access.spi.CreateAccessCertificateRequest;

import java.time.Instant;

@Value
@Builder(builderClassName = "Builder")
public class CreateAccessCertificateRequestImpl implements CreateAccessCertificateRequest {

    @NonNull
    private Issuer issuer;

    @NonNull
    private Application application;

    @NonNull
    private Device device;

    @NonNull
    private Vehicle vehicle;

    @NonNull
    private Instant validFrom;

    @NonNull
    private Instant validUntil;

    @NonNull
    private Permissions permissions;

    @Override
    public String toString() {
        return String.format("CreateAccessCertificateRequestImpl[appId='%s', deviceSerial='%s', vehicleSerial='%s']",
                application.getAppId(), device.getSerialNumber(), vehicle.getSerialNumber());
    }
}
