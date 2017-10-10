package org.amv.access.spi.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Application;
import org.amv.access.core.Device;
import org.amv.access.core.Vehicle;
import org.amv.access.spi.CreateAccessCertificateRequest;

import java.time.LocalDateTime;

@Value
@Builder(builderClassName = "Builder")
public class CreateAccessCertificateRequestImpl implements CreateAccessCertificateRequest {

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

    @Override
    public String toString() {
        return String.format("CreateAccessCertificateRequestImpl[appId='%s', deviceSerial='%s', vehicleSerial='%s']",
                application.getAppId(), device.getSerialNumber(), vehicle.getSerialNumber());
    }
}
