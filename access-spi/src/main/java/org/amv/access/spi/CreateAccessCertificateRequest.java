package org.amv.access.spi;

import org.amv.access.core.*;

import java.time.LocalDateTime;

public interface CreateAccessCertificateRequest {
    Issuer getIssuer();

    /**
     * @return the application requesting the certificate
     */
    Application getApplication();

    /**
     * @return the device requesting the certificate
     */
    Device getDevice();

    /**
     * @return the vehicle the created certificate is for
     */
    Vehicle getVehicle();

    LocalDateTime getValidFrom();

    LocalDateTime getValidUntil();

    Permissions getPermissions();
}
