package org.amv.access.api.access;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.access.model.CreateAccessCertificateRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

@Slf4j
public class CreateAccessCertificateRequestValidator implements Validator {
    private static final int SERIAL_NUMBER_LENGTH = 18;

    @Override
    public boolean supports(Class<?> clazz) {
        return CreateAccessCertificateRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target == null) {
            errors.reject("null", "request must not be null");
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "appId", "appId.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "deviceSerialNumber", "deviceSerialNumber.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "vehicleSerialNumber", "vehicleSerialNumber.empty");

        CreateAccessCertificateRequest request = (CreateAccessCertificateRequest) target;
        boolean isDeviceSerialNumberLengthValid = request != null &&
                request.getDeviceSerialNumber() != null &&
                request.getDeviceSerialNumber().length() == SERIAL_NUMBER_LENGTH;

        if (!isDeviceSerialNumberLengthValid) {
            errors.rejectValue("deviceSerialNumber", "deviceSerialNumber.length");
        }

        boolean isVehicleSerialNumberLengthValid = request != null &&
                request.getVehicleSerialNumber() != null &&
                request.getVehicleSerialNumber().length() == SERIAL_NUMBER_LENGTH;

        if (!isVehicleSerialNumberLengthValid) {
            errors.rejectValue("vehicleSerialNumber", "vehicleSerialNumber.length");
        }

        boolean isValidFromOrUntilMissing = request != null && (request.getValidityStart() == null || request.getValidityEnd() == null);
        if (isValidFromOrUntilMissing) {
            // TODO: while in prototype phase, we can set valid dates ourselves..
            // TODO: but raise error if "valid from" and "valid until" are missing in final release!
            LocalDateTime validFrom = LocalDateTime.now();
            LocalDateTime validUntil = validFrom.plusDays(1)
                    .toLocalDate().atStartOfDay();

            request.setValidityStart(validFrom);
            request.setValidityEnd(validUntil);

            log.warn("Fill missing validity dates of CreateAccessCertificateRequest with: from {}, to {}", validFrom, validUntil);
        }
    }
}
