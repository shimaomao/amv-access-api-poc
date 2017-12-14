package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for adding an access certificate signature.")
public class UpdateAccessCertificateSignatureRequestDto {

    @JsonProperty(value = "vehicle_access_certificate_signature_base64")
    private String vehicleAccessCertificateSignatureBase64;

    @JsonProperty(value = "device_access_certificate_signature_base64")
    private String deviceAccessCertificateSignatureBase64;

    @Tolerate
    protected UpdateAccessCertificateSignatureRequestDto() {

    }
}
