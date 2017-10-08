package org.amv.access.client.model;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.RandomStringUtils;

@VisibleForTesting
public final class DeviceCertificateDtoObjectMother {

    private DeviceCertificateDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static DeviceCertificateDto random() {
        return DeviceCertificateDto.builder()
                .deviceCertificate(RandomStringUtils.randomAlphanumeric(10))
                .issuerPublicKey(RandomStringUtils.randomAlphanumeric(10))
                .build();
    }
}
