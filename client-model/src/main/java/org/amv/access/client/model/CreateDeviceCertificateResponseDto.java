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
    @ApiModelProperty(
            value = "The generated device certificate for this device",
            dataType = "DeviceCertificateDto",
            required = true
    )
    private DeviceCertificateDto deviceCertificate;

    @Tolerate
    protected CreateDeviceCertificateResponseDto() {

    }
}
