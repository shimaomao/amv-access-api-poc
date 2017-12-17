package org.amv.access.certificate;

import org.amv.access.core.AccessCertificate;

import java.util.UUID;

public interface AccessCertificateResource {

    UUID getUuid();

    String getName();

    AccessCertificate getAccessCertificate();
}
