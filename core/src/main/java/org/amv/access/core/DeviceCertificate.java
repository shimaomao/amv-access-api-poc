package org.amv.access.core;

public interface DeviceCertificate {
    Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    // TODO: this value is just for human inspection - not really needed.
    String getCertificateBase64();

    // TODO: this value is just for human inspection - not really needed.
    String getCertificateSignatureBase64();

    String getFullDeviceCertificateBase64();
}
