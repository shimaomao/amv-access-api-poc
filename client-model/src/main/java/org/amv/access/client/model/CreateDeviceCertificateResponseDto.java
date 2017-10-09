package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;

@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class CreateDeviceCertificateResponseDto {

    @JsonProperty("device_certificate")
    @ApiModelProperty(notes = "The generated device certificate for this device", dataType = "String (encoded in base64)")
    private String deviceCertificate;

    @JsonProperty("issuer_public_key")
    @ApiModelProperty(notes = "You may return car model or license plate number", dataType = "String")
    private String issuerPublicKey;

    @Tolerate
    protected CreateDeviceCertificateResponseDto() {

    }
}
