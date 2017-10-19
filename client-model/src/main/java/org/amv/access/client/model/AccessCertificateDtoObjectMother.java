package org.amv.access.client.model;

import org.apache.commons.lang3.RandomStringUtils;

public final class AccessCertificateDtoObjectMother {

    private AccessCertificateDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static AccessCertificateDto random() {
        return AccessCertificateDto.builder()
                .id(RandomStringUtils.randomAlphanumeric(10))
                .deviceAccessCertificate(RandomStringUtils.randomAlphanumeric(10))
                .vehicleAccessCertificate(RandomStringUtils.randomAlphanumeric(10))
                .name(RandomStringUtils.randomAlphanumeric(10))
                .build();
    }
}
