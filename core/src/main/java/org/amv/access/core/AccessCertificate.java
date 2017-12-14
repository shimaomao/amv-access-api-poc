package org.amv.access.core;

public interface AccessCertificate {

    String getUuid();

    String getName();

    /*Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    Vehicle getVehicle();*/

    String getDeviceAccessCertificateBase64();

    String getVehicleAccessCertificateBase64();

}
