package org.amv.access.api.device.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for creating a device certificate.")
public class CreateDeviceCertificateRequest {

    @JsonProperty(value = "app_id")
    @ApiModelProperty(notes = "The ID of the Application whose issuer has to sign the certificate",
            required = true)
    private String appId;

    @JsonProperty(value = "public_key")
    @ApiModelProperty(notes = "Base64 encoded string of the 64-byte public key of the key-pair that has been generated on the device",
            required = true)
    private String publicKey;

    @JsonProperty(value = "name")
    @ApiModelProperty(notes = "An optional friendly name for the device", required = false)
    private String name;
}
