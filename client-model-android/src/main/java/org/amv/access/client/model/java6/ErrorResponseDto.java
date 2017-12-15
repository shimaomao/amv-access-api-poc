package org.amv.access.client.model.java6;

import java.util.List;

public final class ErrorResponseDto {
    public List<ErrorInfoDto> errors;

    public static final class ErrorInfoDto {
        public String title;
        public String source;
        public String detail;
    }
}
