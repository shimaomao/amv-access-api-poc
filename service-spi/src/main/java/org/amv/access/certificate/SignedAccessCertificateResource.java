package org.amv.access.certificate;

import org.amv.access.core.SignedAccessCertificate;

import java.util.UUID;

public interface SignedAccessCertificateResource {

    UUID getUuid();

    String getName();

    SignedAccessCertificate getSignedAccessCertificate();

}
