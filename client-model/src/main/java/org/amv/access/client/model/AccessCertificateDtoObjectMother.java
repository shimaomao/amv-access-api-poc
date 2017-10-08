package org.amv.access.client.model;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.RandomStringUtils;

@VisibleForTesting
public final class AccessCertificateDtoObjectMother {

    private AccessCertificateDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static AccessCertificateDto random() {
        return AccessCertificateDto.builder()
                .id(RandomStringUtils.randomAlphanumeric(10))
                .accessCertificate(RandomStringUtils.randomAlphanumeric(10))
                .name(RandomStringUtils.randomAlphanumeric(10))
                .build();
    }
}
