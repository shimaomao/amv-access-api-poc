package org.amv.access.api.device.deprecated;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Deprecated
@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing a device certificate.")
public class DeviceCertificateDto {

    @JsonProperty(value = "id")
    @ApiModelProperty(notes = "the unique ID of the device certificate", required = true)
    private Long id;

    @JsonProperty(value = "certificate")
    @ApiModelProperty(notes = "the full device certificate in binary format, encoded in base64", required = true)
    private String certificate;

    @JsonProperty(value = "app_id")
    @ApiModelProperty(notes = " the ID of the AmvAccessApplication whose issuer signed the certificate", required = true)
    private String appId;

    @JsonProperty(value = "issuer_name")
    @ApiModelProperty(notes = "the name of the issuer that signed the certificate", required = true)
    private String issuerName;

    @JsonProperty(value = "issuer_public_key")
    @ApiModelProperty(notes = " the public key of the issuer that signed the certificate", required = true)
    private String issuerPublicKey;

    @JsonProperty(value = "device_name")
    @ApiModelProperty(notes = "the friendly name given to the device, if any", required = false)
    private String deviceName;

    @JsonProperty(value = "device_serial_number")
    @ApiModelProperty(notes = "an unique identifier given to the device", required = true)
    private String deviceSerialNumber;
}
