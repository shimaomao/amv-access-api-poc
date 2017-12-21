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
        return String.format("CreateAccessCertificateRequestImpl[deviceSerial='%s', vehicleSerial='%s']",
                device.getSerialNumber(), vehicle.getSerialNumber());
    }
}
