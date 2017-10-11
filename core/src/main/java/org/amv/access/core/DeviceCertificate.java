package org.amv.access.core;

public interface DeviceCertificate {
    Issuer getIssuer();

    Application getApplication();

    Device getDevice();

    String getCertificateBase64();

    String getSignedCertificateBase64();
}
