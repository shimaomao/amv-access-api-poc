package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for creating an access certificate.")
public class CreateAccessCertificateRequestDto {

    @JsonProperty(value = "app_id")
    private String appId;

    @JsonProperty(value = "device_serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 9-byte serial number of the device")
    private String deviceSerialNumber;

    @JsonProperty(value = "vehicle_serial_number")
    @ApiModelProperty(notes = "Base64 encoded string of the 9-byte serial number of the vehicle")
    private String vehicleSerialNumber;

    @JsonProperty(value = "validity_start")
    @ApiModelProperty(notes = "DateTime of when the certificate validity starts")
    private Instant validityStart;

    @JsonProperty(value = "validity_end")
    @ApiModelProperty(notes = "DateTime of when the certificate validity expires")
    private Instant validityEnd;

    @Tolerate
    protected CreateAccessCertificateRequestDto() {

    }
}
