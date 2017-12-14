package org.amv.access.core;

public interface SignedAccessCertificate {

    AccessCertificate getAccessCertificate();

    String getSignedDeviceAccessCertificateBase64();

    String getSignedVehicleAccessCertificateBase64();

}
