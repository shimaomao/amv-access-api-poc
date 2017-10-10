package org.amv.access.api.access.model;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the response after creating an access certificate.")
public class CreateAccessCertificateResponse {

    @NonNull
    private AccessCertificateDto accessCertificate;
}
