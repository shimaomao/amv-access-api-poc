package org.amv.access.client.model;

import com.google.common.collect.ImmutableList;

public final class GetAccessCertificatesResponseDtoObjectMother {

    private GetAccessCertificatesResponseDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static GetAccessCertificatesResponseDto random() {
        return GetAccessCertificatesResponseDto.builder()
                .accessCertificates(ImmutableList.<AccessCertificateDto>builder()
                        .add(AccessCertificateDtoObjectMother.random())
                        .add(AccessCertificateDtoObjectMother.random())
                        .add(AccessCertificateDtoObjectMother.random())
                        .build())
                .build();
    }
}
