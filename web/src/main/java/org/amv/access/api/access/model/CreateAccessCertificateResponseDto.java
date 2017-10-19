package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Tolerate;
import org.amv.access.client.model.AccessCertificateDto;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the response after creating an access certificate.")
public class CreateAccessCertificateResponseDto {

    @JsonProperty(value = "access_certificate")
    private AccessCertificateDto accessCertificate;

    @Tolerate
    protected CreateAccessCertificateResponseDto() {

    }
}
