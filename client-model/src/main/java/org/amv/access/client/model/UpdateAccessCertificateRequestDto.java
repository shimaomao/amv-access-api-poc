package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the request for adding an access certificate signature.")
public class UpdateAccessCertificateRequestDto {

    @JsonProperty(value = "vehicle_access_certificate_signature_base64")
    private String vehicleAccessCertificateSignatureBase64;

    @JsonProperty(value = "signed_vehicle_access_certificate_base64")
    private String signedVehicleAccessCertificateBase64;

    @JsonProperty(value = "device_access_certificate_signature_base64")
    private String deviceAccessCertificateSignatureBase64;

    @JsonProperty(value = "signed_device_access_certificate_base64")
    private String signedDeviceAccessCertificateBase64;

    @Tolerate
    protected UpdateAccessCertificateRequestDto() {

    }
}
