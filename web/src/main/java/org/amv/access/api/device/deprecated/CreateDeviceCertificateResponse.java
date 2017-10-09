package org.amv.access.api.device.deprecated;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

@Deprecated
@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing the response after creating a device certificate.")
public class CreateDeviceCertificateResponse {

    /**
     * {@link lombok.NonNull} cannot be used here, because of
     * {@link JsonUnwrapped}
     * needs value to be nullable -_-
     */
    @JsonUnwrapped
    private DeviceCertificateDto deviceCertificate;
}
