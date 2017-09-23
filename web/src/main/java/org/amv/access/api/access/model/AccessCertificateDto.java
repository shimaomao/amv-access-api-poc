package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing an access certificate.")
public class AccessCertificateDto {
    @JsonProperty(value = "vehicle_access_certificate")
    @ApiModelProperty(notes = "the full access certificate for the vehicle in binary format", dataType = "String (encoded in base64)")
    private String vehicleAccessCertificate;

    @JsonProperty(value = "device_access_certificate")
    @ApiModelProperty(notes = "the full access certificate for the device in binary format", dataType = "String (encoded in base64)")
    private String deviceAccessCertificate;
}
