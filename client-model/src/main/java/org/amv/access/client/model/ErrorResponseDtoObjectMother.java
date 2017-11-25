package org.amv.access.client.model;

import org.apache.commons.lang3.RandomStringUtils;

public final class ErrorResponseDtoObjectMother {

    private ErrorResponseDtoObjectMother() {
        throw new UnsupportedOperationException();
    }

    public static ErrorResponseDto random() {
        return ErrorResponseDto.builder()
                .addError(randomErrorInfo())
                .addError(randomErrorInfo())
                .addError(randomErrorInfo())
                .build();
    }

    private static ErrorResponseDto.ErrorInfoDto randomErrorInfo() {
        String randomString = RandomStringUtils.randomAlphanumeric(5);
        return ErrorResponseDto.ErrorInfoDto.builder()
                .title("test title " + randomString)
                .detail("test detail " + randomString)
                .source("test source " + randomString)
                .build();
    }
}
