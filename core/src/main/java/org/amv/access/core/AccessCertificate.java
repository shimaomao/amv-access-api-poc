package org.amv.access.core;

public interface AccessCertificate {

    Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    Vehicle getVehicle();

    String getSignedDeviceAccessCertificateBase64();

    String getSignedVehicleAccessCertificateBase64();

}
