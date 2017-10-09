package org.amv.access.client.model;

public final class CreateDeviceCertificateResponseDtoObjectMother {

    private CreateDeviceCertificateResponseDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static CreateDeviceCertificateResponseDto random() {
        return CreateDeviceCertificateResponseDto.builder()
                .deviceCertificate(DeviceCertificateDtoObjectMother.random())
                .build();
    }
}
