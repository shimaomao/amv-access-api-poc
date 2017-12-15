package org.amv.access.spi;

import org.amv.access.core.Device;
import org.amv.access.core.Permissions;
import org.amv.access.core.Vehicle;

import java.time.Instant;

public interface CreateAccessCertificateRequest {

    Device getDevice();

    Vehicle getVehicle();

    Instant getValidFrom();

    Instant getValidUntil();

    Permissions getPermissions();
}
