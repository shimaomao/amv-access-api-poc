package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the response after creating an access certificate.")
public class CreateAccessCertificateResponse {

    /**
     * {@link lombok.NonNull} cannot be used here, because of
     * {@link com.fasterxml.jackson.annotation.JsonUnwrapped}
     * needs value to be nullable -_-
     */
    @JsonUnwrapped
    private AccessCertificateDto accessCertificate;
}
