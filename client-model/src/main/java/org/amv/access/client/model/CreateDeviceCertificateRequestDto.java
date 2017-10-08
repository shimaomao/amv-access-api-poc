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
public class CreateDeviceCertificateRequestDto {

    @JsonProperty("device_public_key")
    @ApiModelProperty(notes = "Base64 encoded string of the 64-byte public key of the key-pair that has been generated on the device",
            dataType = "String (base64 encoded)",
            required = true)
    private String devicePublicKey;

    @Tolerate
    protected CreateDeviceCertificateRequestDto() {

    }
}
