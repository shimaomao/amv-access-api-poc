package org.amv.access.core;

public interface AccessCertificate {

    String getUuid();

    Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    Vehicle getVehicle();

    // TODO: this value is just for human inspection - not really needed.
    String getDeviceAccessCertificateBase64();

    // TODO: this value is just for human inspection - not really needed.
    String getDeviceAccessCertificateSignatureBase64();

    // TODO: this value is just for human inspection - not really needed.
    String getVehicleAccessCertificateBase64();

    // TODO: this value is just for human inspection - not really needed.
    String getVehicleAccessCertificateSignatureBase64();

    String getSignedDeviceAccessCertificateBase64();

    String getSignedVehicleAccessCertificateBase64();

}
