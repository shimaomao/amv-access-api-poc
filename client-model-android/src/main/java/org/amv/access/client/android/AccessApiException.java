package org.amv.access.client.android;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.amv.access.client.android.model.ErrorResponseDto;

@Value
@EqualsAndHashCode(callSuper = true)
public class AccessApiException extends Exception {
    private final ErrorResponseDto error;

    public AccessApiException(ErrorResponseDto error, Throwable cause) {
        super(cause);
        if (error == null) {
            throw new IllegalArgumentException("`error` must not be null");
        }
        this.error = error;
    }
}