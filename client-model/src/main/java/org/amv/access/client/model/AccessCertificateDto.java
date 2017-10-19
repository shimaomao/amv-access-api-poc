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
public class AccessCertificateDto {

    @JsonProperty("id")
    @ApiModelProperty(
            value = "Recommended to use an ID that is not incremental and not easy to guess",
            dataType = "String",
            required = true
    )
    private String id;

    @JsonProperty("device_access_certificate")
    @ApiModelProperty(
            value = "the full access certificate for the device in binary format",
            dataType = "String (encoded in base64)",
            required = true
    )
    private String deviceAccessCertificate;

    @JsonProperty("vehicle_access_certificate")
    @ApiModelProperty(
            value = "the full access certificate for the vehicle in binary format",
            dataType = "String (encoded in base64)",
            required = true
    )
    private String vehicleAccessCertificate;

    @JsonProperty("name")
    @ApiModelProperty(
            value = "You may return car model or license plate number",
            dataType = "String",
            required = false
    )
    private String name;

    @Tolerate
    protected AccessCertificateDto() {

    }
}
