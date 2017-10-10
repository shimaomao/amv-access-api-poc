package org.amv.access.spi.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.amv.access.core.Application;
import org.amv.access.core.Device;
import org.amv.access.spi.CreateDeviceCertificateRequest;

@Value
@Builder(builderClassName = "Builder")
public class CreateDeviceCertificateRequestImpl implements CreateDeviceCertificateRequest {

    @NonNull
    private Application application;

    @NonNull
    private Device device;

    @Override
    public String toString() {
        return String.format("CreateDeviceCertificateRequestImpl[appId='%s', deviceSerial='%s']",
                application.getAppId(), device.getSerialNumber());
    }
}
