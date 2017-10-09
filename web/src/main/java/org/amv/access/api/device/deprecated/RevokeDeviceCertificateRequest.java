package org.amv.access.api.device.deprecated;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Deprecated
@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for revoking a device certificate.")
public class RevokeDeviceCertificateRequest {

    @JsonProperty(value = "serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 9-byte device serial number")
    private String serialNumber;
}
