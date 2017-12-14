package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for creating an access certificate.")
public class DemoCreateAccessCertificateRequestDto {

    @JsonProperty(value = "issuer_id")
    private String issuerId;

    @JsonProperty(value = "issuer_private_key_base64")
    private String issuerPrivateKeyBase64;

    @JsonProperty(value = "request")
    private CreateAccessCertificateRequestDto request;

    @Tolerate
    protected DemoCreateAccessCertificateRequestDto() {

    }
}
