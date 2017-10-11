package org.amv.access.api.device.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
public class CreateDeviceCertificateRequest {

    private String appId;

    private String devicePublicKeyBase64;

    private String deviceName;
}
