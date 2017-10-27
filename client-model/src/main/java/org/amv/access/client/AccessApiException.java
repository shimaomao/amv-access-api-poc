package org.amv.access.client;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.amv.access.client.model.ErrorResponseDto;

import static com.google.common.base.Preconditions.checkArgument;

@Value
@EqualsAndHashCode(callSuper = true)
public class AccessApiException extends Exception {
    private final ErrorResponseDto error;

    AccessApiException(ErrorResponseDto error, Throwable cause) {
        super(cause);
        checkArgument(error != null, "`error` must not be null");
        this.error = error;
    }
}