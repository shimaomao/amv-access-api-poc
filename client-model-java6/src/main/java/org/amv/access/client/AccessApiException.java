package org.amv.access.client;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.amv.access.client.model.java6.ErrorResponseDto;

@Value
@EqualsAndHashCode(callSuper = true)
public class AccessApiException extends Exception {
    private final ErrorResponseDto error;

    AccessApiException(ErrorResponseDto error, Throwable cause) {
        super(cause);
        if (error == null) {
            throw new IllegalArgumentException("`error` must not be null");
        }
        this.error = error;
    }
}