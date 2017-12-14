package org.amv.access.demo;

import com.google.common.annotations.VisibleForTesting;
import org.amv.access.auth.IssuerNonceAuthentication;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.VehicleEntity;
import reactor.core.publisher.Mono;

public interface DemoService {

    void createDemoDataFromProperties(DemoProperties demoProperties);

    DemoUser getOrCreateDemoUser();

    default IssuerNonceAuthentication createDemoIssuerNonceAuthentication() {
        return createNonceAuthentication(getOrCreateDemoIssuer());
    }

    IssuerNonceAuthentication createNonceAuthentication(IssuerWithKeys issuerWithKeys);

    IssuerWithKeys getOrCreateDemoIssuer();

    ApplicationEntity getOrCreateDemoApplication();

    VehicleEntity getOrCreateDemoVehicle();

    DeviceEntity getOrCreateDemoDevice();

    @VisibleForTesting
    DeviceWithKeys createDemoDeviceWithKeys(ApplicationEntity applicationEntity);

    Mono<AccessCertificate> createDemoAccessCertificateIfNecessary(DeviceCertificate deviceCertificate);
}
