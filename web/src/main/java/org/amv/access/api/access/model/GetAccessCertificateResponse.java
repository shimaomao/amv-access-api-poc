package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the response for fetching an access certificate.")
public class GetAccessCertificateResponse {

    // TODO: should be a list of certs
    @JsonProperty(value = "access_certificate")
    @ApiModelProperty(notes = "Resource representing an Access Certificate")
    private AccessCertificateDto accessCertificate;
}
