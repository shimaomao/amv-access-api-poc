package org.amv.access.client.model;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.RandomStringUtils;

@VisibleForTesting
public final class CreateDeviceCertificateResponseDtoObjectMother {

    private CreateDeviceCertificateResponseDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static CreateDeviceCertificateResponseDto random() {
        return CreateDeviceCertificateResponseDto.builder()
                .deviceCertificate(RandomStringUtils.randomAlphanumeric(10))
                .issuerPublicKey(RandomStringUtils.randomAlphanumeric(10))
                .build();
    }
}
