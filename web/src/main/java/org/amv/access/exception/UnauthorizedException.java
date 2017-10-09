package org.amv.access.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends AmvAccessRuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
