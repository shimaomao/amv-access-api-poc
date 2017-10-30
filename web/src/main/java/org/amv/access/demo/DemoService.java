package org.amv.access.demo;

import com.google.common.annotations.VisibleForTesting;
import org.amv.access.model.ApplicationEntity;
import org.amv.access.model.DeviceEntity;
import org.amv.access.model.IssuerEntity;
import org.amv.access.model.VehicleEntity;

public interface DemoService {

    void createDemoDataFromProperties(DemoProperties demoProperties);

    DemoUser getOrCreateDemoUser();

    IssuerEntity getOrCreateDemoIssuer();

    ApplicationEntity getOrCreateDemoApplication();

    VehicleEntity getOrCreateDemoVehicle();

    DeviceEntity getOrCreateDemoDevice();

    @VisibleForTesting
    DeviceWithKeys createDemoDeviceWithKeys(IssuerEntity issuerEntity, ApplicationEntity applicationEntity);
}
