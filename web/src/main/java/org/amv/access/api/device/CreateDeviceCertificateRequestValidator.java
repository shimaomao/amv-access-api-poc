package org.amv.access.api.device;

import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import static org.apache.commons.codec.binary.Base64.isBase64;

public class CreateDeviceCertificateRequestValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return CreateDeviceCertificateRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target == null) {
            errors.reject("null", "request must not be null");
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "appId", "appId.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "publicKey", "publicKey.empty");

        CreateDeviceCertificateRequest request = (CreateDeviceCertificateRequest) target;
        if (request.getPublicKey() == null || !isBase64(request.getPublicKey())) {
            errors.rejectValue("publicKey", "publicKey.base64");
        }
    }
}
