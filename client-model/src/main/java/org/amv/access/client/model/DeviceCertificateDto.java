package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;

@Data
@Setter(AccessLevel.PROTECTED)
@lombok.Builder(builderClassName = "Builder")
public class DeviceCertificateDto {

    @JsonProperty("device_certificate")
    @ApiModelProperty(
            value = "The generated device certificate for this device",
            dataType = "String (encoded in base64)",
            required = true
    )
    private String deviceCertificate;

    @JsonProperty("issuer_public_key")
    @ApiModelProperty(
            value = "The public key of certificate granting authority",
            dataType = "String",
            required = true
    )
    private String issuerPublicKey;

    @Tolerate
    protected DeviceCertificateDto() {

    }
}
