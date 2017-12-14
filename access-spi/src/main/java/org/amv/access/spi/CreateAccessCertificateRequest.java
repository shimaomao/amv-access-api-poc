package org.amv.access.spi;

import org.amv.access.core.*;

import java.time.Instant;
import java.time.LocalDateTime;

public interface CreateAccessCertificateRequest {
    //Issuer getIssuer();

    ///**
    // * @return the application requesting the certificate
    // */
    //Application getApplication();

    Device getDevice();

    Vehicle getVehicle();

    Instant getValidFrom();

    Instant getValidUntil();

    Permissions getPermissions();
}
