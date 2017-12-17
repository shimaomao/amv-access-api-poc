package org.amv.access.core;

public interface SignedAccessCertificate {

    String getDeviceAccessCertificateBase64();

    String getDeviceAccessCertificateSignatureBase64();

    String getSignedDeviceAccessCertificateBase64();

    String getVehicleAccessCertificateBase64();

    String getVehicleAccessCertificateSignatureBase64();

    String getSignedVehicleAccessCertificateBase64();
}
