package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for creating an access certificate.")
public class CreateAccessCertificateRequestFirstTryNotDocumented {

    @JsonProperty(value = "access_gaining_serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 9-byte serial number of either the device or the vehicle")
    private String accessGainingSerialNumber;

    @JsonProperty(value = "access_providing_serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 64-byte public key of the key-pair that has been generated on the device")
    private String accessProvidingSerialNumber;

    @JsonProperty(value = "permissions")
    @ApiModelProperty(notes = "Base64 encoded string of the 0-16 bytes permissions")
    private String permissions;

    @JsonProperty(value = "validity_start")
    @ApiModelProperty(notes = "DateTime of when the certificate validity starts")
    private LocalDateTime validityStart;

    @JsonProperty(value = "validity_end")
    @ApiModelProperty(notes = "DateTime of when the certificate validity expires")
    private LocalDateTime validityEnd;
}
