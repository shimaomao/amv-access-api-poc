package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the response after creating an access certificate.")
public class CreateAccessCertificateResponseDto {

    @JsonProperty(value = "access_certificate_signing_request")
    private AccessCertificateSigningRequestDto accessCertificateSigningRequest;

    @Tolerate
    protected CreateAccessCertificateResponseDto() {

    }

    @Data
    @lombok.Builder(builderClassName = "Builder")
    public static class AccessCertificateSigningRequestDto {
        @JsonProperty("id")
        private String id;

        @JsonProperty("device_access_certificate")
        private String deviceAccessCertificate;

        @JsonProperty("vehicle_access_certificate")
        private String vehicleAccessCertificate;
    }
}
