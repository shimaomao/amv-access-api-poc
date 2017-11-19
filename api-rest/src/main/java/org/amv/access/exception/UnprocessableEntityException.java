package org.amv.access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class UnprocessableEntityException extends AmvAccessRuntimeException {
    public UnprocessableEntityException(String message) {
        super(message, null);
    }
}
