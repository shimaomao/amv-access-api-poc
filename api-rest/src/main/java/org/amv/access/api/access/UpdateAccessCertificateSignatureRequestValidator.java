package org.amv.access.api.access;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.client.model.CreateAccessCertificateRequestDto;
import org.amv.access.client.model.UpdateAccessCertificateSignatureRequestDto;
import org.amv.access.model.IssuerEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
public class UpdateAccessCertificateSignatureRequestValidator implements Validator {
    @Value
    @Builder
    public static class Context {
        @NonNull
        private IssuerEntity issuerEntity;
        @NonNull
        private UpdateAccessCertificateSignatureRequestDto request;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Context.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target == null) {
            errors.reject("null", "request must not be null");
        }

    }
}
