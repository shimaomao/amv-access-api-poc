package org.amv.access.api.access.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(builderClassName = "Builder")
public class GetAccessCertificateRequest {
    @NonNull
    private String deviceSerialNumber;
}
