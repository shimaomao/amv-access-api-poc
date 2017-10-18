package org.amv.access.core;

public interface DeviceCertificate {
    Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    String getCertificateBase64();

    String getCertificateSignatureBase64();

    String getSignedDeviceCertificateBase64();
}
