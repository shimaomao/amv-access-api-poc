package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for fetching an access certificate.")
public class GetAccessCertificateRequest {
    @JsonProperty(value = "access_gaining_serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 9-byte serial number of either the device or the vehicle", required = true)
    private String accessGainingSerialNumber;

    @JsonProperty(value = "access_providing_serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 64-byte public key of the key-pair that has been generated on the device", required = true)
    private String accessProvidingSerialNumber;
}
