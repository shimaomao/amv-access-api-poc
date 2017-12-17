package org.amv.access.certificate.impl;

import lombok.Builder;
import lombok.Value;
import org.amv.access.certificate.SignedAccessCertificateResource;
import org.amv.access.core.SignedAccessCertificate;

import java.util.UUID;

@Value
@Builder
public class SignedAccessCertificateResourceImpl implements SignedAccessCertificateResource {
    private UUID uuid;
    private String name;
    private SignedAccessCertificate signedAccessCertificate;
}
