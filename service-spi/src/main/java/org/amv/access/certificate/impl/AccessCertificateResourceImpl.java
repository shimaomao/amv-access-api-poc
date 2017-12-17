package org.amv.access.certificate.impl;

import lombok.Builder;
import lombok.Value;
import org.amv.access.certificate.AccessCertificateResource;
import org.amv.access.core.AccessCertificate;

import java.util.UUID;

@Value
@Builder
public class AccessCertificateResourceImpl implements AccessCertificateResource {
    private UUID uuid;
    private String name;
    private AccessCertificate accessCertificate;
}
